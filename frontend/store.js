export const PRICE_LOOKUP_DEBOUNCE_MS = 350;
export const THEME_ICON_SUN = '<circle cx="12" cy="12" r="4"></circle><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9L17 7M7 17l-2.1 2.1"></path>';
export const THEME_ICON_MOON = '<path d="M20 14.5A8.5 8.5 0 1 1 9.5 4 6.8 6.8 0 0 0 20 14.5z"></path>';
export const COLORS = ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#14b8a6", "#ec4899", "#84cc16"];

export const state = {
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

export const ui = {
  navLinks: Array.from(document.querySelectorAll(".nav-link")),
  themeToggleBtn: document.getElementById("themeToggleBtn"),
  themeToggleIcon: document.querySelector("#themeToggleBtn .theme-icon"),
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
