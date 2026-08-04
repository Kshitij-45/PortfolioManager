package com.example.service;

import com.example.exception.EmptyBatchRequestException;
import com.example.exception.InvalidSymbolException;
import com.example.exception.MarketDataParsingException;
import com.example.exception.MarketDataUnavailableException;
import com.example.exception.StockServiceException;
import tools.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Shared base for all Yahoo Finance v8 chart-based market data services.
 */
public abstract class BaseMarketDataService {

    protected static final String YAHOO_CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d";

    protected final RestTemplate restTemplate;

    protected BaseMarketDataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches the "meta" node from the Yahoo Finance v8 chart response for a symbol.
     * Returns null if no result is present in the response.
     * Throws StockServiceException on network / API errors.
     */
    protected JsonNode fetchChartMeta(String symbol) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    YAHOO_CHART_URL, HttpMethod.GET, entity, JsonNode.class, symbol);

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

    protected HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
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
}
