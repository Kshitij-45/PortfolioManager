export function humanizeRecommendation(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "STRONG_BUY") return "Strong Buy";
  if (normalized === "BUY") return "Buy";
  if (normalized === "HOLD") return "Hold";
  return "Avoid";
}

export function recommendationBadgeClass(value) {
  const normalized = String(value || "").toUpperCase();
  if (normalized === "STRONG_BUY") return "strong-buy";
  if (normalized === "BUY") return "buy";
  if (normalized === "HOLD") return "hold";
  return "avoid";
}

export function escapeHtml(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function normalizeAssetType(assetType) {
  const normalized = String(assetType || "").trim().toLowerCase();

  if (normalized === "stock") return "Stock";
  if (normalized === "bond") return "Bond";
  if (normalized === "crypto") return "Crypto";
  if (normalized === "mutual fund") return "Mutual Fund";
  if (normalized === "cash") return "Cash";
  if (normalized === "etf") return "ETF";
  return "Other";
}

export function inferCompanyName(ticker) {
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

export function formatCurrency(value) {
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2
  }).format(value || 0);
}

export function formatNumber(value, decimals = 2) {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: decimals
  }).format(value || 0);
}

export function formatSignedPercent(value) {
  const sign = value >= 0 ? "+" : "";
  return `${sign}${value.toFixed(1)}%`;
}

export function formatDate(dateStr) {
  if (!dateStr) return "-";
  const date = new Date(`${dateStr}T00:00:00`);
  if (Number.isNaN(date.getTime())) return dateStr;
  return new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "numeric" }).format(date);
}

export function formatShortDate(dateStr) {
  if (!dateStr) return "-";
  const date = new Date(`${dateStr}T00:00:00`);
  if (Number.isNaN(date.getTime())) return dateStr;
  return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric" }).format(date);
}

export function parseHoldingDateValue(dateStr) {
  if (!dateStr) {
    return Number.NaN;
  }

  const timestamp = Date.parse(`${dateStr}T00:00:00`);
  return Number.isFinite(timestamp) ? timestamp : Number.NaN;
}

export function getTodayDateValue() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function setCell(root, key, text) {
  const node = root.querySelector(`[data-key="${key}"]`);
  node.textContent = text;
  return node;
}

export function normalizePerformanceRange(range) {
  const normalized = String(range || "").trim().toLowerCase();
  if (normalized === "1w" || normalized === "1m" || normalized === "1y") {
    return normalized;
  }

  return "1m";
}

export function formatRangeLabel(range) {
  if (range === "1w") return "1W";
  if (range === "1y") return "1Y";
  return "1M";
}

export function filterPointsByRange(points, range) {
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

export function mapRecommendationAssetType(assetType) {
  const normalized = String(assetType || "").trim().toLowerCase();

  if (normalized === "stock") return "Stock";
  if (normalized === "crypto") return "Crypto";
  if (normalized === "etf" || normalized === "etf/fund") return "ETF";
  if (normalized === "bond" || normalized === "bond etf") return "Bond";
  if (normalized === "mutual fund") return "Mutual Fund";
  if (normalized === "cash") return "Cash";
  return "Other";
}

export function normalizeMarketSymbol(assetType, symbol) {
  const normalizedSymbol = String(symbol || "").trim().toUpperCase();
  if (!normalizedSymbol) {
    return "";
  }

  if (normalizeAssetType(assetType) !== "Crypto") {
    return normalizedSymbol;
  }

  return normalizedSymbol.includes("-") ? normalizedSymbol : `${normalizedSymbol}-USD`;
}

export function toMarketSymbol(holding) {
  return normalizeMarketSymbol(holding.assetType, holding.ticker);
}

export function extractNumericPrice(payload, fields = []) {
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
