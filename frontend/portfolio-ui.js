import { state, ui, THEME_ICON_MOON, THEME_ICON_SUN, COLORS } from "./store.js";
import {
  escapeHtml,
  formatCurrency,
  formatDate,
  formatNumber,
  formatShortDate,
  formatSignedPercent,
  getTodayDateValue,
  humanizeRecommendation,
  inferCompanyName,
  normalizeAssetType,
  parseHoldingDateValue,
  setCell
} from "./portfolio-utils.js";

const recommendationActionHandlers = {
  onBuyAsset: null,
  onViewDetails: null
};

export function configureRecommendationActions(handlers) {
  recommendationActionHandlers.onBuyAsset = handlers?.onBuyAsset || null;
  recommendationActionHandlers.onViewDetails = handlers?.onViewDetails || null;
}

export function initTheme() {
  const storedTheme = safelyReadTheme();
  const startingTheme = storedTheme || "dark";
  applyTheme(startingTheme);
}

export function onThemeToggle() {
  const currentTheme = document.body.getAttribute("data-theme") || "dark";
  const nextTheme = currentTheme === "dark" ? "light" : "dark";
  applyTheme(nextTheme);
}

function applyTheme(theme) {
  const normalizedTheme = theme === "light" ? "light" : "dark";
  document.body.setAttribute("data-theme", normalizedTheme);

  if (ui.themeToggleIcon) {
    ui.themeToggleIcon.innerHTML = normalizedTheme === "dark" ? THEME_ICON_MOON : THEME_ICON_SUN;
  }

  if (ui.themeToggleBtn) {
    const targetTheme = normalizedTheme === "dark" ? "light" : "dark";
    ui.themeToggleBtn.setAttribute("aria-label", `Switch to ${targetTheme} theme`);
    ui.themeToggleBtn.setAttribute("title", `Switch to ${targetTheme} theme`);
  }

  try {
    window.localStorage.setItem("pm-theme", normalizedTheme);
  } catch {
    // Ignore storage failures; theme still applies for current session.
  }
}

function safelyReadTheme() {
  try {
    const value = window.localStorage.getItem("pm-theme");
    if (value === "light" || value === "dark") {
      return value;
    }
  } catch {
    // Ignore storage read failures.
  }

  return null;
}

export function onSidebarNavClick(event) {
  const target = event.currentTarget;
  if (!(target instanceof HTMLAnchorElement)) {
    return;
  }
  setActiveNav(target.getAttribute("href") || "");
}

export function syncActiveNavFromHash() {
  const currentHash = window.location.hash || "#dashboardSection";
  setActiveNav(currentHash);
}

function setActiveNav(hash) {
  for (const link of ui.navLinks) {
    const href = link.getAttribute("href") || "";
    link.classList.toggle("active", href === hash);
  }
}

export function openAddAssetModal() {
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

export function closeAddAssetModal() {
  ui.addAssetModal.classList.add("hidden");
  ui.addAssetModal.setAttribute("aria-hidden", "true");
  updateModalBodyState();
}

export function openRemoveAssetModal() {
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

export function closeRemoveAssetModal() {
  ui.removeAssetModal.classList.add("hidden");
  ui.removeAssetModal.setAttribute("aria-hidden", "true");
  ui.removeHoldingForm.reset();
  updateModalBodyState();
}

export function openAddMoneyModal() {
  closeAddAssetModal();
  closeRemoveAssetModal();
  clearAddMoneyFormError();
  ui.addMoneyModal.classList.remove("hidden");
  ui.addMoneyModal.setAttribute("aria-hidden", "false");
  updateModalBodyState();
  ui.addMoneyAmountInput.focus();
}

export function closeAddMoneyModal() {
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

export function refreshModalBodyState() {
  updateModalBodyState();
}

export function onModalClick(event) {
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

export function onGlobalKeyDown(event) {
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

export function closeSymbolPerformanceModal() {
  ui.symbolPerformanceModal.classList.add("hidden");
  ui.symbolPerformanceModal.setAttribute("aria-hidden", "true");
  ui.symbolPerformanceStatus.textContent = "";
  updateModalBodyState();
}

export function setActiveRangeButton(activeRange) {
  for (const button of ui.symbolRangeButtons) {
    button.classList.toggle("active", button.dataset.range === activeRange);
  }
}

export function renderAll() {
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
    setCell(clone, "assetType", holding.assetType);
    setCell(clone, "quantity", formatNumber(holding.quantity, 0));
    setCell(clone, "avgPrice", formatCurrency(holding.avgPrice));
    setCell(clone, "currentPrice", formatCurrency(holding.currentPrice));

    const pnlCell = setCell(clone, "pnl", formatCurrency(pnl));
    pnlCell.classList.add(pnl >= 0 ? "positive" : "negative");
    setCell(clone, "purchaseDate", formatDate(holding.purchaseDate));

    ui.holdingsBody.append(clone);
  }
}

export function renderSymbolPerformanceChart() {
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

export function rebuildPerformanceHistory() {
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

export function normalizeHolding(raw) {
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

export function setHoldingFormError(message) {
  ui.holdingFormError.textContent = message;
}

export function clearHoldingFormError() {
  ui.holdingFormError.textContent = "";
}

export function setPriceLookupStatus(message) {
  if (ui.holdingPriceStatus) {
    ui.holdingPriceStatus.textContent = message;
  }
}

export function clearPriceLookupStatus() {
  setPriceLookupStatus("");
}

export function applyTodayPurchaseDate() {
  ui.holdingForm.elements.purchaseDate.value = getTodayDateValue();
}

export function setAddMoneyFormError(message) {
  ui.addMoneyFormError.textContent = message;
}

export function clearAddMoneyFormError() {
  ui.addMoneyFormError.textContent = "";
}

export function onRemoveHoldingSelectChange() {
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

export function syncRemoveHoldingOptions() {
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

export function renderRecommendations(errorMessage = "") {
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

  const buyBtn = document.createElement("button");
  buyBtn.type = "button";
  buyBtn.className = "btn buy-asset-btn";
  buyBtn.textContent = "Buy Asset";
  buyBtn.addEventListener("click", () => {
    if (typeof recommendationActionHandlers.onBuyAsset === "function") {
      recommendationActionHandlers.onBuyAsset(item);
    }
  });

  const detailsBtn = document.createElement("button");
  detailsBtn.type = "button";
  detailsBtn.className = "btn ghost view-details-btn";
  detailsBtn.textContent = "View Details";
  detailsBtn.addEventListener("click", () => {
    if (typeof recommendationActionHandlers.onViewDetails === "function") {
      recommendationActionHandlers.onViewDetails(item);
    }
  });

  actions.append(buyBtn);
  actions.append(detailsBtn);
  card.append(actions);

  return card;
}

function recommendationBadgeClass(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "STRONG_BUY") return "strong-buy";
  if (normalized === "BUY") return "buy";
  if (normalized === "HOLD") return "hold";
  return "avoid";
}
