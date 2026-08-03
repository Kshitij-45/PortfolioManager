const STORAGE_KEYS = {
  holdings: "pm_holdings",
  history: "pm_history",
  balance: "pm_balance"
};

const COLORS = [
  "#7a5b36",
  "#a67c52",
  "#c79d6d",
  "#5f6f52",
  "#8c5a44",
  "#bba07a"
];

const state = {
  holdings: [],
  history: [],
  balance: 10000,
  view: {
    search: "",
    type: "All",
    sort: "valueDesc"
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
  allocationChart: document.getElementById("allocationChart"),
  allocationLegend: document.getElementById("allocationLegend"),
  historyChart: document.getElementById("historyChart")
};

init();

async function init() {
  hydrateFromStorage();
  attachEvents();

  if (state.holdings.length === 0) {
    seedDemoData();
  }

  renderAll();
}

function hydrateFromStorage() {
  state.holdings = parseJson(localStorage.getItem(STORAGE_KEYS.holdings), []);
  state.history = parseJson(localStorage.getItem(STORAGE_KEYS.history), []);
  state.balance = Number(localStorage.getItem(STORAGE_KEYS.balance));
  if (!Number.isFinite(state.balance) || state.balance < 0) {
    state.balance = 10000;
  }
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
  ui.removeHoldingForm.addEventListener("submit", onHoldingRemove);
  ui.refreshPricesBtn.addEventListener("click", onRefreshPrices);
  ui.openAddPanelBtn.addEventListener("click", openAddAssetModal);
  if (ui.openAddFromHoldingsBtn) {
    ui.openAddFromHoldingsBtn.addEventListener("click", openAddAssetModal);
  }
  ui.closeAddAssetModalBtn.addEventListener("click", closeAddAssetModal);
  ui.addAssetModal.addEventListener("click", onModalClick);
  ui.jumpHoldingsBtn.addEventListener("click", openRemoveAssetModal);
  ui.closeRemoveAssetModalBtn.addEventListener("click", closeRemoveAssetModal);
  ui.removeAssetModal.addEventListener("click", onModalClick);
  ui.removeHoldingSelect.addEventListener("change", onRemoveHoldingSelectChange);
  ui.holdingsSearch.addEventListener("input", onViewControlChange);
  ui.holdingsTypeFilter.addEventListener("change", onViewControlChange);
  ui.holdingsSort.addEventListener("change", onViewControlChange);
  document.addEventListener("keydown", onGlobalKeyDown);
  window.addEventListener("hashchange", syncActiveNavFromHash);

  syncActiveNavFromHash();
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
  ui.addAssetModal.classList.remove("hidden");
  ui.addAssetModal.setAttribute("aria-hidden", "false");
  updateModalBodyState();

  const firstInput = ui.holdingForm.querySelector("input[name=\"ticker\"]");
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
  document.body.classList.toggle("modal-open", addOpen || removeOpen || moneyOpen);
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
}

function onAddMoneySubmit(event) {
  event.preventDefault();
  clearAddMoneyFormError();

  const amount = Number(ui.addMoneyAmountInput.value || 0);
  if (!Number.isFinite(amount) || amount <= 0) {
    setAddMoneyFormError("Enter a valid amount greater than 0.");
    return;
  }

  state.balance = Number((state.balance + amount).toFixed(2));
  persistLocalPortfolio();
  renderSummary();
  closeAddMoneyModal();
}

function onViewControlChange() {
  state.view.search = ui.holdingsSearch.value.trim().toUpperCase();
  state.view.type = ui.holdingsTypeFilter.value;
  state.view.sort = ui.holdingsSort.value;
  renderTable();
}

async function onHoldingAdd(event) {
  event.preventDefault();
  clearHoldingFormError();

  const formData = new FormData(ui.holdingForm);

  const holding = {
    id: crypto.randomUUID(),
    ticker: String(formData.get("ticker") || "").trim().toUpperCase(),
    companyName: String(formData.get("companyName") || "").trim(),
    assetType: String(formData.get("assetType") || "Other"),
    quantity: Number(formData.get("quantity") || 0),
    avgPrice: Number(formData.get("avgPrice") || 0),
    currentPrice: Number(formData.get("currentPrice") || formData.get("avgPrice") || 0),
    purchaseDate: String(formData.get("purchaseDate") || "")
  };

  if (!holding.companyName) {
    holding.companyName = inferCompanyName(holding.ticker);
  }

  if (!holding.ticker || holding.quantity <= 0 || holding.avgPrice < 0 || holding.currentPrice < 0) {
    setHoldingFormError("Enter valid symbol, quantity, and prices.");
    return;
  }

  const buyCost = holding.quantity * holding.avgPrice;
  if (holding.assetType === "Stock" && buyCost > state.balance) {
    setHoldingFormError(
      `Insufficient balance. Available ${formatCurrency(state.balance)}, required ${formatCurrency(buyCost)}.`
    );
    return;
  }

  if (holding.assetType === "Stock") {
    state.balance = Number((state.balance - buyCost).toFixed(2));
  }

  state.holdings.push(holding);
  persistLocalPortfolio();

  ui.holdingForm.reset();
  closeAddAssetModal();
  snapshotHistory();
  renderAll();
}

function onRemoveHoldingSelectChange() {
  const selectedId = ui.removeHoldingSelect.value;
  const holding = state.holdings.find((item) => item.id === selectedId);

  if (!holding) {
    ui.removeHoldingAvailable.textContent = "No holding selected.";
    ui.removeQuantityInput.value = "";
    ui.removeQuantityInput.disabled = true;
    ui.removeHoldingSubmit.disabled = true;
    return;
  }

  ui.removeHoldingAvailable.textContent = `Available shares: ${formatNumber(holding.quantity, 4)} (${holding.ticker} - ${holding.companyName})`;
  ui.removeQuantityInput.disabled = false;
  ui.removeQuantityInput.max = String(holding.quantity);
  ui.removeQuantityInput.placeholder = String(Number(holding.quantity.toFixed(4)));
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
    option.value = holding.id;
    option.textContent = `${holding.ticker} - ${holding.companyName} (${formatNumber(holding.quantity, 4)} shares)`;
    ui.removeHoldingSelect.append(option);
  }

  ui.removeHoldingSelect.disabled = false;
  onRemoveHoldingSelectChange();
}

async function onHoldingRemove(event) {
  event.preventDefault();

  const selectedId = ui.removeHoldingSelect.value;
  const quantityToRemove = Number(ui.removeQuantityInput.value || 0);
  const holding = state.holdings.find((item) => item.id === selectedId);

  if (!holding || quantityToRemove <= 0) {
    return;
  }

  if (quantityToRemove > holding.quantity) {
    return;
  }

  const remainingShares = holding.quantity - quantityToRemove;
  if (remainingShares <= 0.0000001) {
    state.holdings = state.holdings.filter((item) => item.id !== selectedId);
  } else {
    holding.quantity = Number(remainingShares.toFixed(8));
  }

  if (holding.assetType === "Stock") {
    const creditedAmount = quantityToRemove * holding.avgPrice;
    state.balance = Number((state.balance + creditedAmount).toFixed(2));
  }

  persistLocalPortfolio();

  closeRemoveAssetModal();
  snapshotHistory();
  renderAll();
}

async function onRefreshPrices() {
  if (state.holdings.length === 0) {
    return;
  }

  for (const holding of state.holdings) {
    const updatedPrice = await fetchSamplePrice(holding.ticker);
    if (updatedPrice > 0) {
      holding.currentPrice = updatedPrice;
    }
  }

  persistLocalPortfolio();

  snapshotHistory();
  renderAll();
}

function renderAll() {
  renderSummary();
  renderTable();
  renderAllocation();
  renderHistoryChart();
  syncRemoveHoldingOptions();
}

function renderSummary() {
  const totalValue = state.holdings.reduce((sum, h) => sum + h.quantity * h.currentPrice, 0);
  const totalCost = state.holdings.reduce((sum, h) => sum + h.quantity * h.avgPrice, 0);
  const pnl = totalValue - totalCost;
  const returnPct = totalCost > 0 ? (pnl / totalCost) * 100 : 0;

  const byType = { Cash: 0, Stock: 0, Bond: 0, Crypto: 0 };
  for (const holding of state.holdings) {
    const value = holding.quantity * holding.currentPrice;
    if (holding.assetType === "Cash") {
      byType.Cash += value;
    } else if (holding.assetType === "Stock" || holding.assetType === "ETF" || holding.assetType === "Other") {
      byType.Stock += value;
    } else if (holding.assetType === "Bond") {
      byType.Bond += value;
    } else if (holding.assetType === "Crypto") {
      byType.Crypto += value;
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
    row.dataset.id = holding.id;

    const marketValue = holding.quantity * holding.currentPrice;
    const pnl = marketValue - holding.quantity * holding.avgPrice;

    setCell(clone, "ticker", holding.ticker);
    setCell(clone, "companyName", holding.companyName);
    setCell(clone, "quantity", formatNumber(holding.quantity, 4));
    setCell(clone, "avgPrice", formatCurrency(holding.avgPrice));
    setCell(clone, "currentPrice", formatCurrency(holding.currentPrice));
    setCell(clone, "purchaseDate", holding.purchaseDate ? formatDate(holding.purchaseDate) : "—");

    const pnlCell = setCell(clone, "pnl", formatCurrency(pnl));
    pnlCell.classList.add(pnl >= 0 ? "positive" : "negative");

    ui.holdingsBody.append(clone);
  }
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

  if (state.history.length < 2) {
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

  ctx.strokeStyle = "#d8cab6";
  ctx.lineWidth = 1;

  for (let i = 0; i <= 4; i += 1) {
    const y = padding.top + (i / 4) * chartHeight;
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
  }

  ctx.strokeStyle = "#7a5b36";
  ctx.lineWidth = 3;
  ctx.beginPath();

  state.history.forEach((point, index) => {
    const x = padding.left + (index / (state.history.length - 1)) * chartWidth;
    const y = padding.top + ((max - point.value) / range) * chartHeight;

    if (index === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });

  ctx.stroke();

  ctx.fillStyle = "#6e5d48";
  ctx.font = "12px IBM Plex Mono";
  ctx.fillText(formatCurrency(max), 6, padding.top + 4);
  ctx.fillText(formatCurrency(min), 6, height - padding.bottom);
  ctx.fillText(state.history[0].label, padding.left, height - 8);
  ctx.fillText(state.history[state.history.length - 1].label, width - 70, height - 8);
}

function drawNoData(ctx, width, height) {
  ctx.fillStyle = "#6e5d48";
  ctx.font = "15px Space Grotesk";
  ctx.fillText("Need at least two snapshots to draw a trend.", 20, height / 2);
}

function snapshotHistory() {
  const totalValue = state.holdings.reduce((sum, h) => sum + h.quantity * h.currentPrice, 0);

  const label = new Date().toLocaleDateString(undefined, {
    month: "short",
    day: "numeric"
  });

  const existing = state.history[state.history.length - 1];
  if (existing && existing.label === label) {
    existing.value = totalValue;
  } else {
    state.history.push({ label, value: totalValue });
    if (state.history.length > 30) {
      state.history = state.history.slice(-30);
    }
  }

  localStorage.setItem(STORAGE_KEYS.history, JSON.stringify(state.history));
}

async function fetchSamplePrice(ticker) {
  try {
    const response = await fetch(
      `https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker=${encodeURIComponent(ticker)}`
    );

    if (!response.ok) {
      return 0;
    }

    const data = await response.json();

    const candidates = [
      data.price,
      data.close,
      data.currentPrice,
      data.regularMarketPrice,
      data?.quote?.regularMarketPrice
    ];

    const value = candidates.find((x) => typeof x === "number" && x > 0);
    return value || 0;
  } catch {
    return 0;
  }
}

function normalizeHolding(raw) {
  if (!raw) {
    return null;
  }

  const normalized = {
    id: String(raw.id || crypto.randomUUID()),
    ticker: String(raw.ticker || raw.stockTicker || "").toUpperCase(),
    companyName: String(raw.companyName || raw.name || "").trim(),
    assetType: String(raw.assetType || raw.type || "Other"),
    quantity: Number(raw.quantity ?? raw.volume ?? 0),
    avgPrice: Number(raw.avgPrice ?? raw.averagePrice ?? 0),
    currentPrice: Number(raw.currentPrice ?? raw.price ?? raw.avgPrice ?? 0)
  };

  if (!normalized.companyName) {
    normalized.companyName = inferCompanyName(normalized.ticker);
  }

  if (!normalized.ticker || normalized.quantity < 0 || normalized.avgPrice < 0 || normalized.currentPrice < 0) {
    return null;
  }

  return normalized;
}

function seedDemoData() {
  state.balance = 15000;
  state.holdings = [
    { id: crypto.randomUUID(), ticker: "AAPL", companyName: "Apple Inc.", assetType: "Stock", quantity: 35, avgPrice: 120, currentPrice: 145.32, purchaseDate: "2023-03-15" },
    { id: crypto.randomUUID(), ticker: "TSLA", companyName: "Tesla Inc.", assetType: "Stock", quantity: 20, avgPrice: 650, currentPrice: 720.15, purchaseDate: "2022-11-20" },
    { id: crypto.randomUUID(), ticker: "AMZN", companyName: "Amazon.com Inc.", assetType: "Stock", quantity: 10, avgPrice: 3100, currentPrice: 3340.5, purchaseDate: "2021-06-10" },
    { id: crypto.randomUUID(), ticker: "GOOGL", companyName: "Alphabet Inc.", assetType: "Stock", quantity: 8, avgPrice: 2500, currentPrice: 2750.75, purchaseDate: "2022-01-05" },
    { id: crypto.randomUUID(), ticker: "BND", companyName: "Vanguard Total Bond", assetType: "Bond", quantity: 25, avgPrice: 68.4, currentPrice: 71.1, purchaseDate: "2023-07-22" },
    { id: crypto.randomUUID(), ticker: "BTC", companyName: "Bitcoin", assetType: "Crypto", quantity: 0.18, avgPrice: 52000, currentPrice: 61000, purchaseDate: "2024-01-30" },
    { id: crypto.randomUUID(), ticker: "CASH", companyName: "Cash Reserve", assetType: "Cash", quantity: 1, avgPrice: 5200, currentPrice: 5200, purchaseDate: "2023-01-01" }
  ];

  persistLocalPortfolio();
  snapshotHistory();
}

function persistLocalPortfolio() {
  localStorage.setItem(STORAGE_KEYS.holdings, JSON.stringify(state.holdings));
  localStorage.setItem(STORAGE_KEYS.balance, String(state.balance));
}

function setHoldingFormError(message) {
  ui.holdingFormError.textContent = message;
}

function clearHoldingFormError() {
  ui.holdingFormError.textContent = "";
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

function parseJson(value, fallback) {
  try {
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
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
  if (!dateStr) return "\u2014";
  const date = new Date(dateStr + "T00:00:00");
  if (isNaN(date.getTime())) return dateStr;
  return new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "numeric" }).format(date);
}
