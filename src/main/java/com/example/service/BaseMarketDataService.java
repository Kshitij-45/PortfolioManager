package com.example.service;

import com.example.dto.MarketCandleDTO;
import com.example.exception.EmptyBatchRequestException;
import com.example.exception.InvalidSymbolException;
import com.example.exception.MarketDataParsingException;
import com.example.exception.MarketDataUnavailableException;
import com.example.exception.StockServiceException;
import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared base for all Yahoo Finance v8 chart-based market data services.
 */
public abstract class BaseMarketDataService {

    private static final Logger log = LoggerFactory.getLogger(BaseMarketDataService.class);

    private static final String YAHOO_CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d";
    private static final String YAHOO_CRUMB_URL =
            "https://query1.finance.yahoo.com/v1/test/getcrumb";
    private static final String YAHOO_CONSENT_URL =
            "https://fc.yahoo.com";
        private static final String YAHOO_HISTORY_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval={interval}&range={range}";

    private volatile String cachedCrumb   = null;
    private volatile String cachedCookies = null;
    private final    Object crumbLock     = new Object();

    protected final RestTemplate restTemplate;

    protected BaseMarketDataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches the "meta" node from the Yahoo Finance v8 chart response for a symbol.
     * Handles crumb/cookie authentication automatically, retrying once on 401.
     */
    protected JsonNode fetchChartMeta(String symbol) {
        ensureCrumb();
        try {
            return doFetchChartMeta(symbol);
        } catch (MarketDataUnavailableException e) {
            // If 401 the crumb may have expired — refresh once and retry
            if (e.getMessage() != null && (e.getMessage().contains("401")
                    || e.getMessage().contains("Unauthorized"))) {
                log.warn("Crumb expired for symbol '{}', refreshing and retrying...", symbol);
                synchronized (crumbLock) {
                    cachedCrumb   = null;
                    cachedCookies = null;
                }
                ensureCrumb();
                return doFetchChartMeta(symbol);
            }
            throw e;
        }
    }

    private JsonNode doFetchChartMeta(String symbol) {
        String url = YAHOO_CHART_URL + (cachedCrumb != null && !cachedCrumb.isBlank()
                ? "&crumb=" + cachedCrumb : "");
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class, symbol);

            JsonNode root = response.getBody();
            if (root == null) {
                throw new MarketDataUnavailableException(symbol, "Empty response from market data provider");
            }

            JsonNode chart = root.path("chart");
            if (chart == null || chart.isMissingNode() || chart.isNull()) {
                throw new MarketDataParsingException(symbol, "Missing chart node in provider response");
            }

            JsonNode error = chart.path("error");
            if (error != null && !error.isMissingNode() && !error.isNull()) {
                throw new MarketDataUnavailableException(symbol, String.valueOf(error));
            }

            JsonNode results = chart.path("result");
            if (results.isArray() && !results.isEmpty()) {
                return results.get(0).path("meta");
            }
            return null;
        } catch (ResourceAccessException e) {
            throw new MarketDataUnavailableException(symbol, e.getMessage());
        } catch (RestClientResponseException e) {
            throw new MarketDataUnavailableException(symbol, "HTTP " + e.getStatusCode().value() + " from provider");
        } catch (MarketDataUnavailableException | MarketDataParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new StockServiceException(symbol, e.getMessage());
        }
    }

    /**
     * Two-step Yahoo Finance authentication:
     *  1. GET https://fc.yahoo.com  →  collect Set-Cookie headers
     *  2. GET /v1/test/getcrumb with those cookies  →  crumb string
     */
    private void ensureCrumb() {
        if (cachedCrumb != null) return;
        synchronized (crumbLock) {
            if (cachedCrumb != null) return;
            try {
                // Step 1 — consent page gives us the required cookies
                ResponseEntity<String> consent = restTemplate.exchange(
                        YAHOO_CONSENT_URL, HttpMethod.GET,
                        new HttpEntity<>(buildHeaders()), String.class);

                List<String> setCookies = consent.getHeaders().get(HttpHeaders.SET_COOKIE);
                if (setCookies != null && !setCookies.isEmpty()) {
                    cachedCookies = setCookies.stream()
                            .map(h -> h.split(";")[0])   // keep only name=value
                            .collect(Collectors.joining("; "));
                }

                // Step 2 — get crumb using the cookies we just collected
                HttpHeaders crumbHeaders = buildHeaders();
                if (cachedCookies != null && !cachedCookies.isBlank()) {
                    crumbHeaders.set(HttpHeaders.COOKIE, cachedCookies);
                }
                ResponseEntity<String> crumbResp = restTemplate.exchange(
                        YAHOO_CRUMB_URL, HttpMethod.GET,
                        new HttpEntity<>(crumbHeaders), String.class);

                String body = crumbResp.getBody();
                if (body != null && !body.isBlank() && !body.toLowerCase().contains("unauthorized")) {
                    cachedCrumb = body.trim().replace("\"", "");
                    log.info("Yahoo Finance crumb acquired: {}", cachedCrumb);
                } else {
                    log.warn("Could not obtain Yahoo Finance crumb, will try without");
                    cachedCrumb = "";  // blank = no crumb but won't re-attempt
                }
            } catch (Exception e) {
                log.warn("Crumb fetch failed ({}), proceeding without crumb", e.getMessage());
                cachedCrumb   = "";
                cachedCookies = "";
            }
        }
    }

    protected HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
        headers.set("Origin", "https://finance.yahoo.com");
        headers.set("Referer", "https://finance.yahoo.com/");
        if (cachedCookies != null && !cachedCookies.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cachedCookies);
        }
        return headers;
    }

    protected String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? jsonNodeToString(n) : null;
    }

    protected String normalizeSymbol(String symbol, String assetType) {
        if (symbol == null || symbol.isBlank()) {
            throw new InvalidSymbolException(assetType);
        }
        return symbol.trim().toUpperCase();
    }

    protected void validateBatchSymbols(List<String> symbols, String assetType) {
        if (symbols == null || symbols.isEmpty()) {
            throw new EmptyBatchRequestException(assetType);
        }
    }

    private String jsonNodeToString(JsonNode node) {
        String raw = String.valueOf(node);
        if (raw.length() >= 2 && raw.charAt(0) == '"' && raw.charAt(raw.length() - 1) == '"') {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    protected BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull() && n.isNumber()) ? n.decimalValue() : null;
    }

    protected BigDecimal firstDecimal(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }

        for (String field : fields) {
            BigDecimal value = decimalOrNull(node, field);
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }

        return null;
    }

    protected Long longOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull() && n.isNumber()) ? n.longValue() : null;
    }

    protected BigDecimal computeChange(BigDecimal price, BigDecimal prevClose) {
        if (price == null || prevClose == null) return null;
        return price.subtract(prevClose).stripTrailingZeros();
    }

    protected BigDecimal computeChangePercent(BigDecimal change, BigDecimal prevClose) {
        if (change == null || prevClose == null || prevClose.compareTo(BigDecimal.ZERO) == 0) return null;
        return change.divide(prevClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).stripTrailingZeros();
    }

    /**
     * Fetches OHLCV history from Yahoo v8 chart endpoint.
     */
    protected List<MarketCandleDTO> fetchDailyHistory(String symbol, String range) {
        String normalizedRange = (range == null || range.isBlank()) ? "6mo" : range.trim().toLowerCase();
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    YAHOO_HISTORY_URL,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class,
                    symbol,
                    "1d",
                    normalizedRange);

            JsonNode root = response.getBody();
            if (root == null) {
                throw new MarketDataUnavailableException(symbol, "Empty response from market data provider");
            }

            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) {
                throw new MarketDataUnavailableException(symbol, "No history data returned by market data provider");
            }

            JsonNode series = result.get(0);
            JsonNode timestamps = series.path("timestamp");
            JsonNode quote = series.path("indicators").path("quote").path(0);
            JsonNode opens = quote.path("open");
            JsonNode highs = quote.path("high");
            JsonNode lows = quote.path("low");
            JsonNode closes = quote.path("close");
            JsonNode volumes = quote.path("volume");

            if (!timestamps.isArray() || !closes.isArray()) {
                throw new MarketDataParsingException(symbol, "Missing timestamp/close arrays in market data");
            }

            int count = timestamps.size();
            List<MarketCandleDTO> candles = new ArrayList<>(count);
            for (int i = 0; i < count; i += 1) {
                JsonNode tsNode = timestamps.get(i);
                if (tsNode == null || !tsNode.isNumber()) {
                    continue;
                }

                BigDecimal close = decimalAt(closes, i);
                if (close == null || close.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                LocalDate date = Instant.ofEpochSecond(tsNode.asLong())
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

                BigDecimal open = defaultPrice(decimalAt(opens, i), close);
                BigDecimal high = defaultPrice(decimalAt(highs, i), close);
                BigDecimal low = defaultPrice(decimalAt(lows, i), close);
                Long volume = longAt(volumes, i);

                candles.add(MarketCandleDTO.builder()
                        .date(date)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume == null || volume < 0 ? 0L : volume)
                        .build());
            }

            if (candles.isEmpty()) {
                throw new MarketDataUnavailableException(symbol, "No valid OHLCV points in market data");
            }

            return candles;
        } catch (MarketDataUnavailableException | MarketDataParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new MarketDataUnavailableException(symbol, e.getMessage());
        }
    }

    private BigDecimal decimalAt(JsonNode node, int index) {
        if (node == null || !node.isArray() || index < 0 || index >= node.size()) {
            return null;
        }

        JsonNode value = node.get(index);
        if (value == null || value.isNull() || !value.isNumber()) {
            return null;
        }

        return value.decimalValue();
    }

    private Long longAt(JsonNode node, int index) {
        if (node == null || !node.isArray() || index < 0 || index >= node.size()) {
            return null;
        }

        JsonNode value = node.get(index);
        if (value == null || value.isNull() || !value.isNumber()) {
            return null;
        }

        return value.longValue();
    }

    private BigDecimal defaultPrice(BigDecimal candidate, BigDecimal fallback) {
        if (candidate == null || candidate.compareTo(BigDecimal.ZERO) <= 0) {
            return fallback;
        }
        return candidate;
    }
}
