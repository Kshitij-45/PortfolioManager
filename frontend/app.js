function resolveApiBase() {
  if (window.PORTFOLIO_API_BASE) {
    return String(window.PORTFOLIO_API_BASE).replace(/\/$/, "");
  }

  const isHttp = window.location.protocol.startsWith("http");
  const isSpringPort = window.location.port === "8080";

  if (isHttp && isSpringPort) {
    return window.location.origin;
  }

  const host = window.location.hostname || "localhost";
  return `http://${host}:8080`;
}

let API_BASE = resolveApiBase();
const API_BASE_CANDIDATES = buildApiBaseCandidates();
const PRICE_LOOKUP_DEBOUNCE_MS = 350;

const COLORS = ["#b7a3e6", "#9f90cc", "#cec2e9", "#a5aad7", "#c8b3df", "#aea1d4"];

const state = {
  holdings: [],
  history: [],
  balance: 0,
  recommendations: {
    stocks: [],
    crypto: [],
    funds: [],
    bonds: []
  },
  priceLookupToken: 0,
  priceLookupTimer: null,
  symbolPerformance: {
    holding: null,
    range: "1m",
    points: []
  },
  view: {
    search: "",
    type: "All",
    sort: "valueDesc"
  },
  recView: {
    search: "",
    assetType: "All",
    risk: "All",
    recommendation: "All",
    sort: "scoreDesc",
    tab: "stocks"
  }
};

const ui = {
  navLinks: Array.from(document.querySelectorAll(".nav-link")),
  holdingForm: document.getElementById("holdingForm"),
  openAddMoneyModalBtn: document.getElementById("openAddMoneyModalBtn"),
  openAddPanelBtn: document.getElementById("openAddPanelBtn"),
  openAddFromHoldingsBtn: document.getElementById("openAddFromHoldingsBtn"),
  addAssetModal: document.getElementById("addAssetModal"),
  closeAddAssetModalBtn: document.getElementById("closeAddAssetModalBtn"),
  removeAssetModal: document.getElementById("removeAssetModal"),
  closeRemoveAssetModalBtn: document.getElementById("closeRemoveAssetModalBtn"),
  addMoneyModal: document.getElementById("addMoneyModal"),
  closeAddMoneyModalBtn: document.getElementById("closeAddMoneyModalBtn"),
  addMoneyForm: document.getElementById("addMoneyForm"),
  addMoneyAmountInput: document.getElementById("addMoneyAmountInput"),
  addMoneyFormError: document.getElementById("addMoneyFormError"),
  addMoneySubmit: document.getElementById("addMoneySubmit"),
  symbolPerformanceModal: document.getElementById("symbolPerformanceModal"),
  closeSymbolPerformanceModalBtn: document.getElementById("closeSymbolPerformanceModalBtn"),
  symbolPerformanceTitle: document.getElementById("symbolPerformanceTitle"),
  symbolPerformanceChart: document.getElementById("symbolPerformanceChart"),
  symbolPerformanceStatus: document.getElementById("symbolPerformanceStatus"),
  symbolRangeButtons: Array.from(document.querySelectorAll(".range-btn")),
  removeHoldingForm: document.getElementById("removeHoldingForm"),
  removeHoldingSelect: document.getElementById("removeHoldingSelect"),
  removeHoldingAvailable: document.getElementById("removeHoldingAvailable"),
  removeQuantityInput: document.getElementById("removeQuantityInput"),
  removeHoldingSubmit: document.getElementById("removeHoldingSubmit"),
  jumpHoldingsBtn: document.getElementById("jumpHoldingsBtn"),
  holdingsBody: document.getElementById("holdingsBody"),
  rowTemplate: document.getElementById("holdingRowTemplate"),
  emptyState: document.getElementById("emptyState"),
  refreshPricesBtn: document.getElementById("refreshPricesBtn"),
  headerTotalValue: document.getElementById("headerTotalValue"),
  headerReturn: document.getElementById("headerReturn"),
  headerAvailableBalance: document.getElementById("headerAvailableBalance"),
  cashValue: document.getElementById("cashValue"),
  stocksValue: document.getElementById("stocksValue"),
  bondsValue: document.getElementById("bondsValue"),
  cryptoValue: document.getElementById("cryptoValue"),
  holdingsSearch: document.getElementById("holdingsSearch"),
  holdingsTypeFilter: document.getElementById("holdingsTypeFilter"),
  holdingsSort: document.getElementById("holdingsSort"),
  holdingFormError: document.getElementById("holdingFormError"),
  holdingPriceStatus: document.getElementById("holdingPriceStatus"),
  allocationChart: document.getElementById("allocationChart"),
  allocationLegend: document.getElementById("allocationLegend"),
  historyChart: document.getElementById("historyChart"),
  refreshRecommendationsBtn: document.getElementById("refreshRecommendationsBtn"),
  recSearch: document.getElementById("recSearch"),
  recAssetTypeFilter: document.getElementById("recAssetTypeFilter"),
  recRiskFilter: document.getElementById("recRiskFilter"),
  recRecommendationFilter: document.getElementById("recRecommendationFilter"),
  recSort: document.getElementById("recSort"),
  aiTabButtons: Array.from(document.querySelectorAll(".ai-tab")),
  recommendationCards: document.getElementById("recommendationCards"),
  recommendationEmptyState: document.getElementById("recommendationEmptyState")
};

init();

async function init() {
  attachEvents();
  ui.removeQuantityInput.step = "1";
  ui.removeQuantityInput.min = "1";
  await Promise.all([refreshPortfolioState(), refreshRecommendations()]);
}

function buildApiBaseCandidates() {
  const candidates = [];
  const pushCandidate = (value) => {
    const candidate = String(value || "").trim().replace(/\/$/, "");
    if (!candidate || candidates.includes(candidate)) {
      return;
    }

    candidates.push(candidate);
  };

  pushCandidate(window.PORTFOLIO_API_BASE);

  if (window.location.protocol.startsWith("http") && window.location.port === "8080") {
    pushCandidate(window.location.origin);
  }

  const host = window.location.hostname || "localhost";
  pushCandidate(`http://${host}:8080`);
  pushCandidate("http://localhost:8080");
  pushCandidate("http://127.0.0.1:8080");

  return candidates;
}

async function apiFetch(path, options) {
  const normalizedPath = String(path || "");
  const candidates = [API_BASE, ...API_BASE_CANDIDATES.filter((candidate) => candidate !== API_BASE)];
  let lastError = null;
  let lastResponse = null;

  for (const base of candidates) {
    try {
      const response = await fetch(`${base}${normalizedPath}`, options);
      if (!response.ok && base !== candidates[candidates.length - 1]) {
        lastResponse = response;
        continue;
      }

      API_BASE = base;
      return response;
    } catch (error) {
      lastError = error;
    }
  }

  if (lastResponse) {
    return lastResponse;
  }

  throw lastError || new Error("Unable to reach backend API.");
}

function attachEvents() {
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
}

async function refreshPortfolioState() {
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

function onSidebarNavClick(event) {
  const target = event.currentTarget;
  if (!(target instanceof HTMLAnchorElement)) {
    return;
  }
  setActiveNav(target.getAttribute("href") || "");
}

function syncActiveNavFromHash() {
  const currentHash = window.location.hash || "#dashboardSection";
  setActiveNav(currentHash);
}

function setActiveNav(hash) {
  for (const link of ui.navLinks) {
    const href = link.getAttribute("href") || "";
    link.classList.toggle("active", href === hash);
  }
}

function openAddAssetModal() {
  closeAddMoneyModal();
  closeRemoveAssetModal();
  clearHoldingFormError();
  applyTodayPurchaseDate();
  clearPriceLookupStatus();
  ui.addAssetModal.classList.remove("hidden");
  ui.addAssetModal.setAttribute("aria-hidden", "false");
  updateModalBodyState();

  const firstInput = ui.holdingForm.querySelector('input[name="ticker"]');
  if (firstInput instanceof HTMLElement) {
    firstInput.focus();
  }
}

function closeAddAssetModal() {
  ui.addAssetModal.classList.add("hidden");
  ui.addAssetModal.setAttribute("aria-hidden", "true");
  updateModalBodyState();
}

function openRemoveAssetModal() {
  closeAddMoneyModal();
  closeAddAssetModal();
  syncRemoveHoldingOptions();
  ui.removeAssetModal.classList.remove("hidden");
  ui.removeAssetModal.setAttribute("aria-hidden", "false");
  updateModalBodyState();

  if (!ui.removeHoldingSelect.disabled) {
    ui.removeHoldingSelect.focus();
  }
}

function closeRemoveAssetModal() {
  ui.removeAssetModal.classList.add("hidden");
  ui.removeAssetModal.setAttribute("aria-hidden", "true");
  ui.removeHoldingForm.reset();
  updateModalBodyState();
}

function openAddMoneyModal() {
  closeAddAssetModal();
  closeRemoveAssetModal();
  clearAddMoneyFormError();
  ui.addMoneyModal.classList.remove("hidden");
  ui.addMoneyModal.setAttribute("aria-hidden", "false");
  updateModalBodyState();
  ui.addMoneyAmountInput.focus();
}

function closeAddMoneyModal() {
  ui.addMoneyModal.classList.add("hidden");
  ui.addMoneyModal.setAttribute("aria-hidden", "true");
  ui.addMoneyForm.reset();
  clearAddMoneyFormError();
  updateModalBodyState();
}

function updateModalBodyState() {
  const addOpen = !ui.addAssetModal.classList.contains("hidden");
  const removeOpen = !ui.removeAssetModal.classList.contains("hidden");
  const moneyOpen = !ui.addMoneyModal.classList.contains("hidden");
  const performanceOpen = !ui.symbolPerformanceModal.classList.contains("hidden");
  document.body.classList.toggle("modal-open", addOpen || removeOpen || moneyOpen || performanceOpen);
}

function onModalClick(event) {
  const target = event.target;
  if (!(target instanceof HTMLElement)) {
    return;
  }

  if (target.dataset.closeModal === "true") {
    const modal = event.currentTarget;
    if (modal === ui.addAssetModal) {
      closeAddAssetModal();
    } else if (modal === ui.removeAssetModal) {
      closeRemoveAssetModal();
    } else if (modal === ui.addMoneyModal) {
      closeAddMoneyModal();
    } else if (modal === ui.symbolPerformanceModal) {
      closeSymbolPerformanceModal();
    }
  }
}

function onGlobalKeyDown(event) {
  if (event.key === "Escape" && !ui.addAssetModal.classList.contains("hidden")) {
    closeAddAssetModal();
  }

  if (event.key === "Escape" && !ui.removeAssetModal.classList.contains("hidden")) {
    closeRemoveAssetModal();
  }

  if (event.key === "Escape" && !ui.addMoneyModal.classList.contains("hidden")) {
    closeAddMoneyModal();
  }

  if (event.key === "Escape" && !ui.symbolPerformanceModal.classList.contains("hidden")) {
    closeSymbolPerformanceModal();
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
  updateModalBodyState();

  await loadSymbolPerformance("1m");
}

function closeSymbolPerformanceModal() {
  ui.symbolPerformanceModal.classList.add("hidden");
  ui.symbolPerformanceModal.setAttribute("aria-hidden", "true");
  ui.symbolPerformanceStatus.textContent = "";
  updateModalBodyState();
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

function setActiveRangeButton(activeRange) {
  for (const button of ui.symbolRangeButtons) {
    button.classList.toggle("active", button.dataset.range === activeRange);
  }
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

async function loadPortfolioSnapshotHistory(holdingId, range) {
  const response = await apiFetch(`/api/portfolios/${holdingId}/history`);
  if (!response.ok) {
    throw new Error(await readApiError(response));
  }

  const payload = await response.json();
  const points = Array.isArray(payload)
    ? payload
      .map((point) => ({
        date: String(point.recordedDate || "").trim(),
        value: Number(point.currentPrice)
      }))
      .filter((point) => point.date && Number.isFinite(point.value) && point.value > 0)
    : [];

  return filterPointsByRange(points, range);
}

function filterPointsByRange(points, range) {
  if (!Array.isArray(points) || points.length === 0) {
    return [];
  }

  const days = range === "1w" ? 7 : range === "1y" ? 365 : 31;
  const cutoff = new Date();
  cutoff.setDate(cutoff.getDate() - days);

  const sorted = [...points].sort((a, b) => a.date.localeCompare(b.date));
  const filtered = sorted.filter((point) => {
    const stamp = new Date(`${point.date}T00:00:00`);
    return !Number.isNaN(stamp.getTime()) && stamp >= cutoff;
  });

  return filtered.length > 0 ? filtered : sorted;
}

function normalizePerformanceRange(range) {
  const normalized = String(range || "").trim().toLowerCase();
  if (normalized === "1w" || normalized === "1m" || normalized === "1y") {
    return normalized;
  }

  return "1m";
}

function formatRangeLabel(range) {
  if (range === "1w") return "1W";
  if (range === "1y") return "1Y";
  return "1M";
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

    renderSummary();
    closeAddMoneyModal();
  } catch (error) {
    setAddMoneyFormError(error.message);
  }
}

function onViewControlChange() {
  state.view.search = ui.holdingsSearch.value.trim().toUpperCase();
  state.view.type = ui.holdingsTypeFilter.value;
  state.view.sort = ui.holdingsSort.value;
  renderTable();
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

async function refreshRecommendations() {
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

function renderRecommendations(errorMessage = "") {
  if (!ui.recommendationCards || !ui.recommendationEmptyState) {
    return;
  }

  const tab = state.recView.tab || "stocks";
  const source = Array.isArray(state.recommendations[tab]) ? state.recommendations[tab] : [];
  const filtered = filterRecommendations(source);
  const sorted = sortRecommendations(filtered);

  ui.recommendationCards.innerHTML = "";

  if (errorMessage) {
    ui.recommendationEmptyState.hidden = false;
    ui.recommendationEmptyState.textContent = errorMessage;
    return;
  }

  if (sorted.length === 0) {
    ui.recommendationEmptyState.hidden = false;
    ui.recommendationEmptyState.textContent = source.length === 0
      ? "No recommendations available right now."
      : "No recommendations match the current filters.";
    return;
  }

  ui.recommendationEmptyState.hidden = true;
  for (const item of sorted) {
    ui.recommendationCards.append(buildRecommendationCard(item));
  }
}

function filterRecommendations(items) {
  const search = state.recView.search;
  const assetType = state.recView.assetType;
  const risk = state.recView.risk;
  const recommendation = state.recView.recommendation;

  return items.filter((item) => {
    const ticker = String(item.ticker || "").toUpperCase();
    const companyName = String(item.companyName || "").toUpperCase();
    const itemAssetType = String(item.assetType || "");
    const itemRisk = String(item.riskLevel || "");
    const itemRecommendation = String(item.recommendation || "");

    const matchesSearch = !search || ticker.includes(search) || companyName.includes(search);
    const matchesAssetType = assetType === "All" || itemAssetType === assetType;
    const matchesRisk = risk === "All" || itemRisk === risk;
    const matchesRecommendation = recommendation === "All" || itemRecommendation === recommendation;

    return matchesSearch && matchesAssetType && matchesRisk && matchesRecommendation;
  });
}

function sortRecommendations(items) {
  const mode = state.recView.sort;
  const sorted = [...items];

  sorted.sort((a, b) => {
    const scoreA = Number(a.score || 0);
    const scoreB = Number(b.score || 0);
    const confidenceA = Number(a.confidence || 0);
    const confidenceB = Number(b.confidence || 0);
    const priceA = Number(a.currentPrice || 0);
    const priceB = Number(b.currentPrice || 0);

    if (mode === "confidenceDesc") {
      return confidenceB - confidenceA;
    }

    if (mode === "priceDesc") {
      return priceB - priceA;
    }

    if (mode === "priceAsc") {
      return priceA - priceB;
    }

    return scoreB - scoreA;
  });

  return sorted;
}

function buildRecommendationCard(item) {
  const card = document.createElement("article");
  card.className = "rec-card";

  const recommendationLabel = humanizeRecommendation(item.recommendation);
  const badgeClass = recommendationBadgeClass(item.recommendation);
  const reasons = Array.isArray(item.reasons) ? item.reasons.slice(0, 4) : [];

  card.innerHTML = `
    <div class="rec-top">
      <div>
        <h3 class="rec-title">${escapeHtml(item.companyName || item.ticker || "Unknown")}</h3>
        <p class="rec-ticker">${escapeHtml(item.ticker || "-")} - ${escapeHtml(item.assetType || "-")}</p>
      </div>
      <span class="rec-badge ${badgeClass}">${recommendationLabel}</span>
    </div>
    <div class="rec-metrics">
      <div><strong>Price:</strong> ${formatCurrency(Number(item.currentPrice || 0))}</div>
      <div><strong>AI Score:</strong> ${Number(item.score || 0)}</div>
      <div><strong>Confidence:</strong> ${Number(item.confidence || 0)}%</div>
      <div><strong>Risk:</strong> ${escapeHtml(item.riskLevel || "-")}</div>
    </div>
  `;

  const reasonList = document.createElement("ul");
  reasonList.className = "rec-reasons";
  for (const reason of reasons) {
    const li = document.createElement("li");
    li.textContent = String(reason || "");
    reasonList.append(li);
  }

  if (reasons.length === 0) {
    const li = document.createElement("li");
    li.textContent = "No strong technical signal currently.";
    reasonList.append(li);
  }

  card.append(reasonList);

  const actions = document.createElement("div");
  actions.className = "rec-actions";

  const detailsBtn = document.createElement("button");
  detailsBtn.type = "button";
  detailsBtn.className = "btn ghost view-details-btn";
  detailsBtn.textContent = "View Details";
  detailsBtn.addEventListener("click", () => {
    const ticker = encodeURIComponent(String(item.ticker || "").trim());
    if (!ticker) {
      return;
    }
    window.open(`https://finance.yahoo.com/quote/${ticker}`, "_blank", "noopener");
  });

  actions.append(detailsBtn);
  card.append(actions);

  return card;
}

function humanizeRecommendation(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "STRONG_BUY") return "Strong Buy";
  if (normalized === "BUY") return "Buy";
  if (normalized === "HOLD") return "Hold";
  return "Avoid";
}

function recommendationBadgeClass(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "STRONG_BUY") return "strong-buy";
  if (normalized === "BUY") return "buy";
  if (normalized === "HOLD") return "hold";
  return "avoid";
}

function escapeHtml(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
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

function onRemoveHoldingSelectChange() {
  const selectedId = Number(ui.removeHoldingSelect.value);
  const holding = state.holdings.find((item) => item.id === selectedId);

  if (!holding) {
    ui.removeHoldingAvailable.textContent = "No holding selected.";
    ui.removeQuantityInput.value = "";
    ui.removeQuantityInput.disabled = true;
    ui.removeHoldingSubmit.disabled = true;
    return;
  }

  ui.removeHoldingAvailable.textContent = `Available shares: ${formatNumber(holding.quantity, 0)} (${holding.ticker} - ${holding.companyName})`;
  ui.removeQuantityInput.disabled = false;
  ui.removeQuantityInput.max = String(holding.quantity);
  ui.removeQuantityInput.placeholder = String(holding.quantity);
  ui.removeHoldingSubmit.disabled = false;
}

function syncRemoveHoldingOptions() {
  ui.removeHoldingSelect.innerHTML = "";

  if (state.holdings.length === 0) {
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "No holdings available";
    ui.removeHoldingSelect.append(option);
    ui.removeHoldingSelect.disabled = true;
    ui.removeQuantityInput.disabled = true;
    ui.removeHoldingSubmit.disabled = true;
    ui.removeHoldingAvailable.textContent = "Add assets before removing them.";
    return;
  }

  const sorted = [...state.holdings].sort((a, b) => a.ticker.localeCompare(b.ticker));
  for (const holding of sorted) {
    const option = document.createElement("option");
    option.value = String(holding.id);
    option.textContent = `${holding.ticker} - ${holding.companyName} (${formatNumber(holding.quantity, 0)} shares)`;
    ui.removeHoldingSelect.append(option);
  }

  ui.removeHoldingSelect.disabled = false;
  onRemoveHoldingSelectChange();
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

async function fetchPriceForHolding(holding) {
  const symbol = toMarketSymbol(holding);
  const assetType = normalizeAssetType(holding.assetType);

  if (assetType === "Cash") {
    return holding.currentPrice;
  }

  const price = await fetchMarketPrice(assetType, symbol);
  return Number.isFinite(price) && price > 0 ? price : holding.currentPrice;
}

async function fetchMarketPrice(assetType, symbol) {
  const endpoint = getMarketPriceEndpoint(assetType, symbol);
  if (!endpoint) {
    return 0;
  }

  try {
    const response = await apiFetch(endpoint);
    if (response.ok) {
      const payload = await parseApiPayload(response);
      const directPrice = extractNumericPrice(payload);
      if (Number.isFinite(directPrice) && directPrice > 0) {
        return directPrice;
      }
    }
  } catch {
    // Fallback handled below.
  }

  const quoteEndpoint = getMarketQuoteEndpoint(assetType, symbol);
  if (!quoteEndpoint) {
    return 0;
  }

  try {
    const quoteResponse = await apiFetch(quoteEndpoint);
    if (!quoteResponse.ok) {
      return 0;
    }

    const quotePayload = await parseApiPayload(quoteResponse);
    const quotePrice = extractNumericPrice(
      quotePayload,
      ["currentPrice", "nav", "regularMarketPrice", "price"]
    );

    return Number.isFinite(quotePrice) && quotePrice > 0 ? quotePrice : 0;
  } catch {
    return 0;
  }
}

async function parseApiPayload(response) {
  const raw = await response.text();
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

function getMarketPriceEndpoint(assetType, symbol) {
  if (!symbol) {
    return "";
  }

  const encodedSymbol = encodeURIComponent(symbol);
  if (assetType === "Bond") {
    return `/api/bonds/${encodedSymbol}/price`;
  }

  if (assetType === "Crypto") {
    return `/api/crypto/${encodedSymbol}/price`;
  }

  if (assetType === "Mutual Fund") {
    return `/api/funds/${encodedSymbol}/nav`;
  }

  return `/api/stocks/${encodedSymbol}/price`;
}

function getMarketQuoteEndpoint(assetType, symbol) {
  if (!symbol) {
    return "";
  }

  const encodedSymbol = encodeURIComponent(symbol);
  if (assetType === "Bond") {
    return `/api/bonds/${encodedSymbol}`;
  }

  if (assetType === "Crypto") {
    return `/api/crypto/${encodedSymbol}`;
  }

  if (assetType === "Mutual Fund") {
    return `/api/funds/${encodedSymbol}`;
  }

  return `/api/stocks/${encodedSymbol}`;
}

function extractNumericPrice(payload, fields = []) {
  if (typeof payload === "number") {
    return payload;
  }

  if (typeof payload === "string") {
    const parsed = Number(payload);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  if (payload && typeof payload === "object") {
    for (const field of fields) {
      if (Object.prototype.hasOwnProperty.call(payload, field)) {
        const parsed = Number(payload[field]);
        if (Number.isFinite(parsed)) {
          return parsed;
        }
      }
    }

    const parsed = Number(payload.value ?? payload.price ?? payload.currentPrice);
    if (Number.isFinite(parsed)) {
      return parsed;
    }

    for (const value of Object.values(payload)) {
      const nested = extractNumericPrice(value, fields);
      if (Number.isFinite(nested) && nested > 0) {
        return nested;
      }
    }

    return 0;
  }

  return 0;
}

function toMarketSymbol(holding) {
  return normalizeMarketSymbol(holding.assetType, holding.ticker);
}

function normalizeMarketSymbol(assetType, symbol) {
  const normalizedSymbol = String(symbol || "").trim().toUpperCase();
  if (!normalizedSymbol) {
    return "";
  }

  if (normalizeAssetType(assetType) !== "Crypto") {
    return normalizedSymbol;
  }

  return normalizedSymbol.includes("-") ? normalizedSymbol : `${normalizedSymbol}-USD`;
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

function renderAll() {
  renderSummary();
  renderTable();
  renderAllocation();
  renderHistoryChart();
  renderRecommendations();
  syncRemoveHoldingOptions();
}

function renderSummary() {
  const totalValue = state.holdings.reduce((sum, h) => sum + h.quantity * h.currentPrice, 0);
  const totalCost = state.holdings.reduce((sum, h) => sum + h.quantity * h.avgPrice, 0);
  const pnl = totalValue - totalCost;
  const returnPct = totalCost > 0 ? (pnl / totalCost) * 100 : 0;

  const byType = { Stock: 0, Bond: 0, Crypto: 0 };
  for (const holding of state.holdings) {
    const value = holding.quantity * holding.currentPrice;
    const normalizedType = (holding.assetType || "").toLowerCase();

    if (normalizedType === "bond") {
      byType.Bond += value;
    } else if (normalizedType === "crypto") {
      byType.Crypto += value;
    } else {
      byType.Stock += value;
    }
  }

  ui.headerTotalValue.textContent = formatCurrency(totalValue);
  ui.headerReturn.textContent = formatSignedPercent(returnPct);
  ui.headerReturn.style.color = returnPct >= 0 ? "var(--good)" : "var(--bad)";
  ui.headerAvailableBalance.textContent = formatCurrency(state.balance);

  ui.cashValue.textContent = formatCurrency(state.balance);
  ui.stocksValue.textContent = formatCurrency(byType.Stock);
  ui.bondsValue.textContent = formatCurrency(byType.Bond);
  ui.cryptoValue.textContent = formatCurrency(byType.Crypto);
}

function renderTable() {
  ui.holdingsBody.innerHTML = "";
  const visibleHoldings = getVisibleHoldings();

  if (visibleHoldings.length === 0) {
    ui.emptyState.hidden = false;
    if (state.holdings.length === 0) {
      ui.emptyState.textContent = "No holdings yet. Add your first asset above.";
    } else {
      ui.emptyState.textContent = "No holdings match your current filter/search.";
    }
    return;
  }

  ui.emptyState.hidden = true;

  for (const holding of visibleHoldings) {
    const clone = ui.rowTemplate.content.cloneNode(true);
    const row = clone.querySelector("tr");
    row.dataset.id = String(holding.id);

    const marketValue = holding.quantity * holding.currentPrice;
    const pnl = marketValue - holding.quantity * holding.avgPrice;

    const tickerCell = setCell(clone, "ticker", "");
    const symbolButton = document.createElement("button");
    symbolButton.type = "button";
    symbolButton.className = "symbol-trigger";
    symbolButton.dataset.holdingId = String(holding.id);
    symbolButton.textContent = holding.ticker;
    symbolButton.setAttribute("aria-label", `View ${holding.ticker} price performance chart`);
    tickerCell.append(symbolButton);

    setCell(clone, "companyName", holding.companyName);
    setCell(clone, "quantity", formatNumber(holding.quantity, 0));
    setCell(clone, "avgPrice", formatCurrency(holding.avgPrice));
    setCell(clone, "currentPrice", formatCurrency(holding.currentPrice));

    const pnlCell = setCell(clone, "pnl", formatCurrency(pnl));
    pnlCell.classList.add(pnl >= 0 ? "positive" : "negative");

    ui.holdingsBody.append(clone);
  }
}

function renderSymbolPerformanceChart() {
  const ctx = ui.symbolPerformanceChart.getContext("2d");
  const width = ui.symbolPerformanceChart.width;
  const height = ui.symbolPerformanceChart.height;

  ctx.clearRect(0, 0, width, height);

  if (state.symbolPerformance.points.length === 0) {
    drawSymbolNoData(ctx, width, height);
    return;
  }

  const values = state.symbolPerformance.points.map((point) => point.value);
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = Math.max(0.01, max - min);

  const padding = { top: 20, right: 20, bottom: 30, left: 60 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  ctx.strokeStyle = "#3a3e52";
  ctx.lineWidth = 1;
  for (let i = 0; i <= 4; i += 1) {
    const y = padding.top + (i / 4) * chartHeight;
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
  }

  ctx.strokeStyle = "#b7a3e6";
  ctx.lineWidth = 3;
  ctx.beginPath();

  const pointCount = state.symbolPerformance.points.length;
  state.symbolPerformance.points.forEach((point, index) => {
    const x = pointCount === 1
      ? padding.left + chartWidth / 2
      : padding.left + (index / (pointCount - 1)) * chartWidth;
    const y = padding.top + ((max - point.value) / range) * chartHeight;

    if (index === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });
  ctx.stroke();

  const firstPoint = state.symbolPerformance.points[0];
  const lastPoint = state.symbolPerformance.points[pointCount - 1];
  ctx.fillStyle = "#b4bdd6";
  ctx.font = "12px IBM Plex Mono";
  ctx.fillText(formatCurrency(max), 6, padding.top + 4);
  ctx.fillText(formatCurrency(min), 6, height - padding.bottom);
  ctx.fillText(formatShortDate(firstPoint.date), padding.left, height - 8);
  ctx.fillText(formatShortDate(lastPoint.date), width - 76, height - 8);
}

function drawSymbolNoData(ctx, width, height) {
  ctx.fillStyle = "#b4bdd6";
  ctx.font = "15px Space Grotesk";
  ctx.fillText("Click a range to load historical price data.", 20, height / 2);
}

function getVisibleHoldings() {
  const search = state.view.search;
  const selectedType = state.view.type;
  const sort = state.view.sort;

  const filtered = state.holdings.filter((holding) => {
    const companyName = String(holding.companyName || "").toUpperCase();
    const matchesSearch = !search || holding.ticker.includes(search) || companyName.includes(search);
    const matchesType = selectedType === "All" || holding.assetType === selectedType;
    return matchesSearch && matchesType;
  });

  const sorted = [...filtered];
  sorted.sort((a, b) => {
    const valueA = a.quantity * a.currentPrice;
    const valueB = b.quantity * b.currentPrice;
    const pnlA = valueA - a.quantity * a.avgPrice;
    const pnlB = valueB - b.quantity * b.avgPrice;

    if (sort === "pnlDesc") {
      return pnlB - pnlA;
    }

    if (sort === "pnlAsc") {
      return pnlA - pnlB;
    }

    if (sort === "tickerAsc") {
      return a.ticker.localeCompare(b.ticker);
    }

    return valueB - valueA;
  });

  return sorted;
}

function renderAllocation() {
  ui.allocationLegend.innerHTML = "";

  if (state.holdings.length === 0) {
    ui.allocationChart.innerHTML = '<p class="muted">Add holdings to see allocation.</p>';
    return;
  }

  const totalsByType = {};
  for (const h of state.holdings) {
    const value = h.quantity * h.currentPrice;
    totalsByType[h.assetType] = (totalsByType[h.assetType] || 0) + value;
  }

  const entries = Object.entries(totalsByType).sort((a, b) => b[1] - a[1]);
  const total = entries.reduce((sum, [, value]) => sum + value, 0);

  let runningPercent = 0;
  const parts = entries.map(([name, value], i) => {
    const rawPercent = (value / total) * 100;
    const bounded = i === entries.length - 1 ? 100 - runningPercent : rawPercent;
    runningPercent += bounded;
    return {
      name,
      value,
      percent: Math.max(0, bounded),
      color: COLORS[i % COLORS.length]
    };
  });

  const gradient = parts
    .map((p, i) => {
      const start = parts.slice(0, i).reduce((sum, x) => sum + x.percent, 0);
      const end = start + p.percent;
      return `${p.color} ${start}% ${end}%`;
    })
    .join(", ");

  ui.allocationChart.innerHTML = `<div class="donut" style="background: conic-gradient(${gradient});" aria-label="Allocation donut chart"></div>`;

  for (const part of parts) {
    const li = document.createElement("li");
    li.innerHTML = `<span><span class="swatch" style="background:${part.color}"></span>${part.name}</span><strong>${part.percent.toFixed(1)}%</strong>`;
    ui.allocationLegend.append(li);
  }
}

function renderHistoryChart() {
  const ctx = ui.historyChart.getContext("2d");
  const width = ui.historyChart.width;
  const height = ui.historyChart.height;

  ctx.clearRect(0, 0, width, height);

  if (state.history.length === 0) {
    drawNoData(ctx, width, height);
    return;
  }

  const values = state.history.map((h) => h.value);
  const max = Math.max(...values);
  const min = Math.min(...values);
  const range = Math.max(1, max - min);

  const padding = { top: 20, right: 20, bottom: 30, left: 50 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  ctx.strokeStyle = "#3a3e52";
  ctx.lineWidth = 1;

  for (let i = 0; i <= 4; i += 1) {
    const y = padding.top + (i / 4) * chartHeight;
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
  }

  ctx.strokeStyle = "#b7a3e6";
  ctx.lineWidth = 3;
  ctx.beginPath();

  const pointCount = state.history.length;

  state.history.forEach((point, index) => {
    const x = pointCount === 1
      ? padding.left + chartWidth / 2
      : padding.left + (index / (pointCount - 1)) * chartWidth;
    const y = padding.top + ((max - point.value) / range) * chartHeight;

    if (index === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });

  ctx.stroke();

  if (pointCount === 1) {
    const point = state.history[0];
    const x = padding.left + chartWidth / 2;
    const y = padding.top + ((max - point.value) / range) * chartHeight;

    ctx.fillStyle = "#b7a3e6";
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.fillStyle = "#b4bdd6";
  ctx.font = "12px IBM Plex Mono";
  ctx.fillText(formatCurrency(max), 6, padding.top + 4);
  ctx.fillText(formatCurrency(min), 6, height - padding.bottom);
  ctx.fillText(state.history[0].label, padding.left, height - 8);
  ctx.fillText(state.history[state.history.length - 1].label, width - 70, height - 8);
}

function drawNoData(ctx, width, height) {
  ctx.fillStyle = "#b4bdd6";
  ctx.font = "15px Space Grotesk";
  ctx.fillText("Add holdings with purchase dates to plot profit/loss.", 20, height / 2);
}

function rebuildPerformanceHistory() {
  const datedHoldings = state.holdings
    .map((holding) => ({
      holding,
      timestamp: parseHoldingDateValue(holding.purchaseDate)
    }))
    .filter((entry) => Number.isFinite(entry.timestamp))
    .sort((a, b) => a.timestamp - b.timestamp);

  if (datedHoldings.length === 0) {
    state.history = [];
    return;
  }

  const totalsByDate = new Map();
  let cumulativePnl = 0;

  for (const entry of datedHoldings) {
    const pnl = entry.holding.quantity * (entry.holding.currentPrice - entry.holding.avgPrice);
    cumulativePnl += pnl;

    const dateKey = entry.holding.purchaseDate;
    totalsByDate.set(dateKey, cumulativePnl);
  }

  state.history = Array.from(totalsByDate, ([dateKey, value]) => ({
    label: formatShortDate(dateKey),
    value
  }));
}

function normalizeHolding(raw) {
  if (!raw) {
    return null;
  }

  const id = Number(raw.id);
  const ticker = String(raw.symbol || raw.ticker || "").trim().toUpperCase();
  const quantity = Number(raw.quantity);
  const avgPrice = Number(raw.buyPrice ?? raw.avgPrice ?? 0);
  const currentPrice = Number(raw.currentPrice ?? avgPrice);

  if (!Number.isInteger(id) || id <= 0 || !ticker || !Number.isInteger(quantity) || quantity <= 0) {
    return null;
  }

  return {
    id,
    ticker,
    companyName: String(raw.companyName || inferCompanyName(ticker)).trim(),
    assetType: normalizeAssetType(String(raw.assetType || "Other")),
    quantity,
    avgPrice,
    currentPrice,
    purchaseDate: String(raw.purchaseDate || "")
  };
}

function normalizeAssetType(assetType) {
  const normalized = String(assetType || "").trim().toLowerCase();

  if (normalized === "stock") return "Stock";
  if (normalized === "bond") return "Bond";
  if (normalized === "crypto") return "Crypto";
  if (normalized === "mutual fund") return "Mutual Fund";
  if (normalized === "cash") return "Cash";
  if (normalized === "etf") return "ETF";
  return "Other";
}

function setHoldingFormError(message) {
  ui.holdingFormError.textContent = message;
}

function clearHoldingFormError() {
  ui.holdingFormError.textContent = "";
}

function setPriceLookupStatus(message) {
  if (ui.holdingPriceStatus) {
    ui.holdingPriceStatus.textContent = message;
  }
}

function clearPriceLookupStatus() {
  setPriceLookupStatus("");
}

function getTodayDateValue() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function applyTodayPurchaseDate() {
  ui.holdingForm.elements.purchaseDate.value = getTodayDateValue();
}

function setAddMoneyFormError(message) {
  ui.addMoneyFormError.textContent = message;
}

function clearAddMoneyFormError() {
  ui.addMoneyFormError.textContent = "";
}

function setCell(root, key, text) {
  const node = root.querySelector(`[data-key="${key}"]`);
  node.textContent = text;
  return node;
}

async function readApiError(response) {
  try {
    const payload = await response.json();
    return payload.message || payload.error || `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
  }
}

function inferCompanyName(ticker) {
  const names = {
    AAPL: "Apple Inc.",
    TSLA: "Tesla Inc.",
    AMZN: "Amazon.com Inc.",
    GOOGL: "Alphabet Inc.",
    META: "Meta Platforms Inc.",
    MSFT: "Microsoft Corp.",
    NVDA: "NVIDIA Corp.",
    BND: "Bond Fund",
    BTC: "Bitcoin",
    ETH: "Ethereum",
    CASH: "Cash Reserve"
  };

  return names[ticker] || `${ticker} Holdings`;
}

function formatCurrency(value) {
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2
  }).format(value || 0);
}

function formatNumber(value, decimals = 2) {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: decimals
  }).format(value || 0);
}

function formatSignedPercent(value) {
  const sign = value >= 0 ? "+" : "";
  return `${sign}${value.toFixed(1)}%`;
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  const date = new Date(dateStr + "T00:00:00");
  if (Number.isNaN(date.getTime())) return dateStr;
  return new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "numeric" }).format(date);
}

function formatShortDate(dateStr) {
  if (!dateStr) return "-";
  const date = new Date(dateStr + "T00:00:00");
  if (Number.isNaN(date.getTime())) return dateStr;
  return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric" }).format(date);
}

function parseHoldingDateValue(dateStr) {
  if (!dateStr) {
    return Number.NaN;
  }

  const timestamp = Date.parse(`${dateStr}T00:00:00`);
  return Number.isFinite(timestamp) ? timestamp : Number.NaN;
}
