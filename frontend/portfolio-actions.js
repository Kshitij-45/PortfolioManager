import { state, ui, PRICE_LOOKUP_DEBOUNCE_MS } from "./store.js";
import {
  formatCurrency,
  formatRangeLabel,
  formatSignedPercent,
  getTodayDateValue,
  inferCompanyName,
  mapRecommendationAssetType,
  normalizeAssetType,
  normalizeMarketSymbol,
  normalizePerformanceRange
} from "./portfolio-utils.js";
import {
  apiFetch,
  fetchMarketPrice,
  fetchPriceForHolding,
  loadPortfolioSnapshotHistory,
  readApiError
} from "./portfolio-api.js";
import {
  applyTodayPurchaseDate,
  clearHoldingFormError,
  clearAddMoneyFormError,
  clearPriceLookupStatus,
  closeAddAssetModal,
  closeAddMoneyModal,
  closeSymbolPerformanceModal,
  closeRemoveAssetModal,
  configureRecommendationActions,
  onGlobalKeyDown,
  onModalClick,
  onRemoveHoldingSelectChange,
  onSidebarNavClick,
  openAddAssetModal,
  openAddMoneyModal,
  openRemoveAssetModal,
  rebuildPerformanceHistory,
  renderAll,
  renderRecommendations,
  renderSymbolPerformanceChart,
  refreshModalBodyState,
  setActiveRangeButton,
  setAddMoneyFormError,
  setHoldingFormError,
  setPriceLookupStatus,
  syncActiveNavFromHash,
  normalizeHolding
} from "./portfolio-ui.js";

export function attachEvents(onThemeToggle) {
  if (ui.themeToggleBtn) {
    ui.themeToggleBtn.addEventListener("click", onThemeToggle);
  }

  for (const link of ui.navLinks) {
    link.addEventListener("click", onSidebarNavClick);
  }

  ui.openAddMoneyModalBtn.addEventListener("click", openAddMoneyModal);
  ui.addMoneyForm.addEventListener("submit", onAddMoneySubmit);
  ui.closeAddMoneyModalBtn.addEventListener("click", closeAddMoneyModal);
  ui.addMoneyModal.addEventListener("click", onModalClick);

  ui.holdingForm.addEventListener("submit", onHoldingAdd);
  ui.holdingForm.elements.ticker.addEventListener("input", onHoldingLookupInput);
  ui.holdingForm.elements.assetType.addEventListener("change", onHoldingLookupInput);
  ui.holdingForm.elements.avgPrice.addEventListener("input", onAvgPriceInput);
  ui.removeHoldingForm.addEventListener("submit", onHoldingRemove);
  ui.refreshPricesBtn.addEventListener("click", onRefreshPrices);
  ui.holdingsBody.addEventListener("click", onHoldingsTableClick);

  ui.openAddPanelBtn.addEventListener("click", openAddAssetModal);
  if (ui.openAddFromHoldingsBtn) {
    ui.openAddFromHoldingsBtn.addEventListener("click", openAddAssetModal);
  }

  ui.closeAddAssetModalBtn.addEventListener("click", closeAddAssetModal);
  ui.addAssetModal.addEventListener("click", onModalClick);

  ui.closeSymbolPerformanceModalBtn.addEventListener("click", closeSymbolPerformanceModal);
  ui.symbolPerformanceModal.addEventListener("click", onModalClick);
  ui.symbolRangeButtons.forEach((button) => {
    button.addEventListener("click", onSymbolRangeClick);
  });

  ui.jumpHoldingsBtn.addEventListener("click", openRemoveAssetModal);
  ui.closeRemoveAssetModalBtn.addEventListener("click", closeRemoveAssetModal);
  ui.removeAssetModal.addEventListener("click", onModalClick);
  ui.removeHoldingSelect.addEventListener("change", onRemoveHoldingSelectChange);

  ui.holdingsSearch.addEventListener("input", onViewControlChange);
  ui.holdingsTypeFilter.addEventListener("change", onViewControlChange);
  ui.holdingsSort.addEventListener("change", onViewControlChange);

  if (ui.refreshRecommendationsBtn) {
    ui.refreshRecommendationsBtn.addEventListener("click", () => {
      void refreshRecommendations();
    });
  }

  if (ui.recSearch) {
    ui.recSearch.addEventListener("input", onRecommendationViewChange);
  }

  if (ui.recRiskFilter) {
    ui.recRiskFilter.addEventListener("change", onRecommendationViewChange);
  }

  if (ui.recAssetTypeFilter) {
    ui.recAssetTypeFilter.addEventListener("change", onRecommendationViewChange);
  }

  if (ui.recRecommendationFilter) {
    ui.recRecommendationFilter.addEventListener("change", onRecommendationViewChange);
  }

  if (ui.recSort) {
    ui.recSort.addEventListener("change", onRecommendationViewChange);
  }

  if (ui.aiTabButtons.length > 0) {
    ui.aiTabButtons.forEach((button) => {
      button.addEventListener("click", onRecommendationTabClick);
    });
  }

  document.addEventListener("keydown", onGlobalKeyDown);
  window.addEventListener("hashchange", syncActiveNavFromHash);

  syncActiveNavFromHash();

  configureRecommendationActions({
    onBuyAsset: (item) => {
      openAddAssetModal();
      prefillHoldingFormFromRecommendation(item);
    },
    onViewDetails: (item) => {
      const ticker = encodeURIComponent(String(item?.ticker || "").trim());
      if (!ticker) {
        return;
      }
      window.open(`https://finance.yahoo.com/quote/${ticker}`, "_blank", "noopener");
    }
  });
}

export async function refreshPortfolioState() {
  try {
    const [portfoliosResponse, balanceResponse] = await Promise.all([
      apiFetch("/api/portfolios"),
      apiFetch("/api/balance")
    ]);

    if (!portfoliosResponse.ok) {
      throw new Error(await readApiError(portfoliosResponse));
    }

    if (!balanceResponse.ok) {
      throw new Error(await readApiError(balanceResponse));
    }

    const portfolios = await portfoliosResponse.json();
    const balance = await balanceResponse.json();

    state.holdings = Array.isArray(portfolios)
      ? portfolios.map(normalizeHolding).filter(Boolean)
      : [];
    state.balance = Number(balance && balance.availableBalance);
    if (!Number.isFinite(state.balance) || state.balance < 0) {
      state.balance = 0;
    }

    rebuildPerformanceHistory();
    renderAll();
  } catch (error) {
    setHoldingFormError(`Unable to load backend data: ${error.message}`);
    state.holdings = [];
    state.balance = 0;
    state.history = [];
    renderAll();
  }
}

export async function refreshRecommendations() {
  if (!ui.refreshRecommendationsBtn) {
    return;
  }

  ui.refreshRecommendationsBtn.disabled = true;
  ui.refreshRecommendationsBtn.textContent = "Refreshing...";

  try {
    const response = await apiFetch("/api/recommendations");
    if (!response.ok) {
      throw new Error(await readApiError(response));
    }

    const payload = await response.json();
    state.recommendations = {
      stocks: Array.isArray(payload?.stocks) ? payload.stocks : [],
      crypto: Array.isArray(payload?.crypto) ? payload.crypto : [],
      funds: Array.isArray(payload?.funds) ? payload.funds : [],
      bonds: Array.isArray(payload?.bonds) ? payload.bonds : []
    };

    renderRecommendations();
  } catch (error) {
    state.recommendations = { stocks: [], crypto: [], funds: [], bonds: [] };
    renderRecommendations(`Unable to load recommendations: ${error.message}`);
  } finally {
    ui.refreshRecommendationsBtn.disabled = false;
    ui.refreshRecommendationsBtn.textContent = "Refresh Suggestions";
  }
}

function onViewControlChange() {
  state.view.search = ui.holdingsSearch.value.trim().toUpperCase();
  state.view.type = ui.holdingsTypeFilter.value;
  state.view.sort = ui.holdingsSort.value;
  renderAll();
}

function onRecommendationViewChange() {
  state.recView.search = String(ui.recSearch?.value || "").trim().toUpperCase();
  state.recView.assetType = String(ui.recAssetTypeFilter?.value || "All");
  state.recView.risk = String(ui.recRiskFilter?.value || "All");
  state.recView.recommendation = String(ui.recRecommendationFilter?.value || "All");
  state.recView.sort = String(ui.recSort?.value || "scoreDesc");
  renderRecommendations();
}

function onRecommendationTabClick(event) {
  const target = event.currentTarget;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const nextTab = String(target.dataset.tab || "stocks");
  state.recView.tab = nextTab;
  ui.aiTabButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.tab === nextTab);
  });

  renderRecommendations();
}

async function onAddMoneySubmit(event) {
  event.preventDefault();
  clearAddMoneyFormError();

  const amount = Number(ui.addMoneyAmountInput.value || 0);
  if (!Number.isFinite(amount) || amount <= 0) {
    setAddMoneyFormError("Enter a valid amount greater than 0.");
    return;
  }

  try {
    const response = await apiFetch("/api/balance/add", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ amount })
    });

    if (!response.ok) {
      throw new Error(await readApiError(response));
    }

    const payload = await response.json();
    state.balance = Number(payload.availableBalance || state.balance);
    if (!Number.isFinite(state.balance) || state.balance < 0) {
      state.balance = 0;
    }

    renderAll();
    closeAddMoneyModal();
  } catch (error) {
    setAddMoneyFormError(error.message);
  }
}

async function onHoldingAdd(event) {
  event.preventDefault();
  clearHoldingFormError();

  const formData = new FormData(ui.holdingForm);

  const ticker = String(formData.get("ticker") || "").trim().toUpperCase();
  const companyName = String(formData.get("companyName") || "").trim() || inferCompanyName(ticker);
  const assetType = normalizeAssetType(String(formData.get("assetType") || "Other"));
  const quantity = Number.parseInt(String(formData.get("quantity") || "0"), 10);
  const avgPrice = Number(formData.get("avgPrice") || 0);
  const currentPrice = Number(formData.get("currentPrice") || 0);
  const purchaseDate = String(formData.get("purchaseDate") || "").trim() || getTodayDateValue();

  if (!ticker || !companyName) {
    setHoldingFormError("Symbol and company name are required.");
    return;
  }

  if (!Number.isInteger(quantity) || quantity <= 0) {
    setHoldingFormError("Quantity must be a whole number greater than 0.");
    return;
  }

  if (!Number.isFinite(avgPrice) || avgPrice <= 0) {
    setHoldingFormError("Buy price must be greater than 0.");
    return;
  }

  if (assetType !== "Cash" && (!Number.isFinite(currentPrice) || currentPrice <= 0)) {
    setHoldingFormError("Unable to fetch current market price. Check the symbol and try again.");
    return;
  }

  const effectiveCurrentPrice = assetType === "Cash" ? avgPrice : currentPrice;
  if (!Number.isFinite(effectiveCurrentPrice) || effectiveCurrentPrice <= 0) {
    setHoldingFormError("Current price is not valid.");
    return;
  }

  try {
    const response = await apiFetch("/api/portfolios", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        symbol: ticker,
        companyName,
        assetType,
        quantity,
        buyPrice: avgPrice,
        currentPrice: effectiveCurrentPrice,
        purchaseDate
      })
    });

    if (!response.ok) {
      throw new Error(await readApiError(response));
    }

    ui.holdingForm.reset();
    closeAddAssetModal();
    await refreshPortfolioState();
  } catch (error) {
    setHoldingFormError(error.message);
  }
}

function onHoldingLookupInput(event) {
  const tickerInput = ui.holdingForm.elements.ticker;
  if (event && event.target === tickerInput) {
    tickerInput.value = tickerInput.value.toUpperCase().trimStart();
  }

  applyTodayPurchaseDate();

  if (state.priceLookupTimer) {
    window.clearTimeout(state.priceLookupTimer);
  }

  state.priceLookupTimer = window.setTimeout(() => {
    void lookupAndFillCurrentPrice();
  }, PRICE_LOOKUP_DEBOUNCE_MS);
}

function onAvgPriceInput() {
  const assetType = normalizeAssetType(String(ui.holdingForm.elements.assetType.value || "Other"));
  if (assetType !== "Cash") {
    return;
  }

  const avgPrice = Number(ui.holdingForm.elements.avgPrice.value || 0);
  if (Number.isFinite(avgPrice) && avgPrice > 0) {
    ui.holdingForm.elements.currentPrice.value = avgPrice.toFixed(2);
  }
}

async function lookupAndFillCurrentPrice() {
  const ticker = String(ui.holdingForm.elements.ticker.value || "").trim().toUpperCase();
  const assetType = normalizeAssetType(String(ui.holdingForm.elements.assetType.value || "Other"));
  const currentPriceInput = ui.holdingForm.elements.currentPrice;

  if (!ticker) {
    currentPriceInput.value = "";
    setPriceLookupStatus("Enter a symbol to fetch market price.");
    return;
  }

  if (assetType === "Cash") {
    onAvgPriceInput();
    setPriceLookupStatus("Current price follows cost basis for cash.");
    return;
  }

  const token = ++state.priceLookupToken;
  const marketSymbol = normalizeMarketSymbol(assetType, ticker);
  setPriceLookupStatus(`Fetching ${assetType.toLowerCase()} price for ${ticker}...`);

  try {
    const nextPrice = await fetchMarketPrice(assetType, marketSymbol);
    if (token !== state.priceLookupToken) {
      return;
    }

    if (!Number.isFinite(nextPrice) || nextPrice <= 0) {
      currentPriceInput.value = "";
      setPriceLookupStatus(`Price unavailable for ${ticker}.`);
      return;
    }

    currentPriceInput.value = nextPrice.toFixed(2);
    setPriceLookupStatus(`Live price loaded for ${ticker}.`);
  } catch {
    if (token !== state.priceLookupToken) {
      return;
    }

    currentPriceInput.value = "";
    setPriceLookupStatus(`Could not fetch price for ${ticker}.`);
  }
}

async function onHoldingRemove(event) {
  event.preventDefault();

  const selectedId = Number(ui.removeHoldingSelect.value);
  const quantityToRemove = Number.parseInt(ui.removeQuantityInput.value || "0", 10);
  const holding = state.holdings.find((item) => item.id === selectedId);

  if (!holding || !Number.isInteger(quantityToRemove) || quantityToRemove <= 0) {
    return;
  }

  if (quantityToRemove > holding.quantity) {
    return;
  }

  try {
    if (quantityToRemove === holding.quantity) {
      const response = await apiFetch(`/api/portfolios/${holding.id}`, {
        method: "DELETE"
      });

      if (!response.ok) {
        throw new Error(await readApiError(response));
      }
    } else {
      const response = await apiFetch(`/api/portfolios/${holding.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(buildPortfolioPayload(holding, {
          quantity: holding.quantity - quantityToRemove
        }))
      });

      if (!response.ok) {
        throw new Error(await readApiError(response));
      }
    }

    closeRemoveAssetModal();
    await refreshPortfolioState();
  } catch (error) {
    ui.removeHoldingAvailable.textContent = error.message;
  }
}

async function onRefreshPrices() {
  if (state.holdings.length === 0) {
    return;
  }

  ui.refreshPricesBtn.disabled = true;
  ui.refreshPricesBtn.textContent = "Refreshing...";

  try {
    let changed = false;
    for (const holding of state.holdings) {
      try {
        const nextPrice = await fetchPriceForHolding(holding);
        if (!Number.isFinite(nextPrice) || nextPrice <= 0 || nextPrice === holding.currentPrice) {
          continue;
        }

        const response = await apiFetch(`/api/portfolios/${holding.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(buildPortfolioPayload(holding, { currentPrice: nextPrice }))
        });

        if (!response.ok) {
          continue;
        }

        changed = true;
      } catch {
        // Skip symbols that fail price lookup/update so one bad symbol doesn't block refresh.
        continue;
      }
    }

    if (changed) {
      await refreshPortfolioState();
    } else {
      renderAll();
    }
  } catch (error) {
    setHoldingFormError(`Price refresh failed: ${error.message}`);
  } finally {
    ui.refreshPricesBtn.disabled = false;
    ui.refreshPricesBtn.textContent = "Refresh";
  }
}

function onHoldingsTableClick(event) {
  const target = event.target;
  if (!(target instanceof HTMLElement)) {
    return;
  }

  const trigger = target.closest(".symbol-trigger");
  if (!(trigger instanceof HTMLButtonElement)) {
    return;
  }

  const holdingId = Number(trigger.dataset.holdingId || 0);
  const holding = state.holdings.find((item) => item.id === holdingId);
  if (!holding) {
    return;
  }

  void openSymbolPerformanceModal(holding);
}

async function openSymbolPerformanceModal(holding) {
  closeAddAssetModal();
  closeRemoveAssetModal();
  closeAddMoneyModal();

  state.symbolPerformance.holding = holding;
  state.symbolPerformance.range = "1m";
  state.symbolPerformance.points = [];

  ui.symbolPerformanceTitle.textContent = `${holding.ticker} Performance`;
  ui.symbolPerformanceStatus.textContent = "";
  ui.symbolPerformanceModal.classList.remove("hidden");
  ui.symbolPerformanceModal.setAttribute("aria-hidden", "false");
  setActiveRangeButton("1m");
  renderSymbolPerformanceChart();
  refreshModalBodyState();

  await loadSymbolPerformance("1m");
}

function onSymbolRangeClick(event) {
  const target = event.currentTarget;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const range = String(target.dataset.range || "").toLowerCase();
  if (!range || range === state.symbolPerformance.range) {
    return;
  }

  void loadSymbolPerformance(range);
}

async function loadSymbolPerformance(range) {
  const holding = state.symbolPerformance.holding;
  if (!holding) {
    return;
  }

  const safeRange = normalizePerformanceRange(range);
  state.symbolPerformance.range = safeRange;
  setActiveRangeButton(safeRange);
  ui.symbolPerformanceStatus.textContent = `Loading ${holding.ticker} history (${safeRange.toUpperCase()})...`;

  let usedSnapshotFallback = false;
  let points = [];

  try {
    const response = await apiFetch(
      `/api/stocks/${encodeURIComponent(holding.ticker)}/history?range=${encodeURIComponent(safeRange)}`
    );

    if (!response.ok) {
      throw new Error(await readApiError(response));
    }

    const payload = await response.json();
    points = Array.isArray(payload)
      ? payload
        .map((point) => ({
          date: String(point.date || "").trim(),
          value: Number(point.closePrice)
        }))
        .filter((point) => point.date && Number.isFinite(point.value) && point.value > 0)
      : [];
  } catch (error) {
    try {
      points = await loadPortfolioSnapshotHistory(holding.id, safeRange);
      usedSnapshotFallback = true;
    } catch {
      state.symbolPerformance.points = [];
      renderSymbolPerformanceChart();
      ui.symbolPerformanceStatus.textContent = `Unable to load price history: ${error.message}`;
      return;
    }
  }

  state.symbolPerformance.points = points;
  renderSymbolPerformanceChart();

  if (points.length === 0) {
    ui.symbolPerformanceStatus.textContent = "No market data available for this range.";
    return;
  }

  const first = points[0];
  const last = points[points.length - 1];
  const change = ((last.value - first.value) / first.value) * 100;
  const suffix = usedSnapshotFallback ? " (from saved portfolio snapshots)" : "";
  ui.symbolPerformanceStatus.textContent = `${formatRangeLabel(safeRange)} change: ${formatSignedPercent(change)} (${formatCurrency(first.value)} -> ${formatCurrency(last.value)})${suffix}`;
}

function buildPortfolioPayload(holding, overrides = {}) {
  return {
    symbol: holding.ticker,
    companyName: holding.companyName,
    assetType: holding.assetType,
    quantity: Number.isInteger(overrides.quantity) ? overrides.quantity : holding.quantity,
    buyPrice: Number.isFinite(overrides.avgPrice) ? overrides.avgPrice : holding.avgPrice,
    currentPrice: Number.isFinite(overrides.currentPrice) ? overrides.currentPrice : holding.currentPrice,
    purchaseDate: holding.purchaseDate
  };
}

function prefillHoldingFormFromRecommendation(item) {
  const ticker = String(item?.ticker || "").trim().toUpperCase();
  const companyName = String(item?.companyName || "").trim();
  const mappedAssetType = mapRecommendationAssetType(String(item?.assetType || ""));
  const currentPrice = Number(item?.currentPrice || 0);

  ui.holdingForm.elements.ticker.value = ticker;
  ui.holdingForm.elements.companyName.value = companyName;
  ui.holdingForm.elements.assetType.value = mappedAssetType;
  ui.holdingForm.elements.quantity.value = "1";

  if (Number.isFinite(currentPrice) && currentPrice > 0) {
    const formattedPrice = currentPrice.toFixed(2);
    ui.holdingForm.elements.avgPrice.value = formattedPrice;
    ui.holdingForm.elements.currentPrice.value = formattedPrice;
    setPriceLookupStatus(`Prefilled market price for ${ticker || "asset"}.`);
  } else {
    ui.holdingForm.elements.avgPrice.value = "";
    ui.holdingForm.elements.currentPrice.value = "";
    setPriceLookupStatus("Price unavailable from suggestion. Enter or fetch market price.");
  }

  applyTodayPurchaseDate();
}
