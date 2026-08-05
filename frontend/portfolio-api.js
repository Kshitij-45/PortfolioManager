import {
  extractNumericPrice,
  filterPointsByRange,
  normalizeAssetType,
  toMarketSymbol
} from "./portfolio-utils.js";

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

let API_BASE = resolveApiBase();
const API_BASE_CANDIDATES = buildApiBaseCandidates();

export async function apiFetch(path, options) {
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

export async function parseApiPayload(response) {
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

export async function readApiError(response) {
  try {
    const payload = await response.json();
    return payload.message || payload.error || `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
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

export async function fetchMarketPrice(assetType, symbol) {
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

export async function fetchPriceForHolding(holding) {
  const symbol = toMarketSymbol(holding);
  const assetType = normalizeAssetType(holding.assetType);

  if (assetType === "Cash") {
    return holding.currentPrice;
  }

  const price = await fetchMarketPrice(assetType, symbol);
  return Number.isFinite(price) && price > 0 ? price : holding.currentPrice;
}

export async function loadPortfolioSnapshotHistory(holdingId, range) {
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
