package com.example.service;

import com.example.Exception.StockNotFoundException;
import com.example.Exception.StockServiceException;
import com.example.dto.StockQuoteDTO;
import tools.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    private static final String YAHOO_CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d";

    private final RestTemplate restTemplate;

    public StockService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get a full quote for a single stock symbol (e.g. "AAPL", "TSLA").
     */
    public StockQuoteDTO getQuote(String symbol) {
        String upperSymbol = symbol.trim().toUpperCase();
        List<StockQuoteDTO> results = fetchQuotes(upperSymbol);
        if (results.isEmpty()) {
            throw new StockNotFoundException(upperSymbol);
        }
        return results.get(0);
    }

    /**
     * Get the current price only for a symbol.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        return getQuote(symbol).getCurrentPrice();
    }

    /**
     * Get quotes for multiple symbols at once.
     */
    public List<StockQuoteDTO> getQuotes(List<String> symbols) {
        List<StockQuoteDTO> results = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                results.add(getQuote(symbol));
            } catch (StockNotFoundException ignored) {
                // skip invalid symbols in batch
            }
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private List<StockQuoteDTO> fetchQuotes(String symbol) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    YAHOO_CHART_URL, HttpMethod.GET, entity, JsonNode.class, symbol);
            return parseChartResponse(response.getBody(), symbol);
        } catch (HttpClientErrorException.NotFound e) {
            throw new StockNotFoundException(symbol);
        } catch (StockNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new StockServiceException(symbol, e.getMessage());
        }
    }

    private List<StockQuoteDTO> parseChartResponse(JsonNode root, String symbol) {
        if (root == null) {
            throw new StockServiceException(symbol, "Empty response from Yahoo Finance");
        }
        JsonNode error = root.path("chart").path("error");
        if (!error.isNull() && !error.isMissingNode()) {
            throw new StockServiceException(symbol, error.asText());
        }
        JsonNode results = root.path("chart").path("result");
        List<StockQuoteDTO> list = new ArrayList<>();
        if (results.isArray() && !results.isEmpty()) {
            list.add(mapMetaToDTO(results.get(0).path("meta")));
        }
        return list;
    }

    private StockQuoteDTO mapMetaToDTO(JsonNode meta) {
        BigDecimal price    = decimalOrNull(meta, "regularMarketPrice");
        BigDecimal prevClose = decimalOrNull(meta, "chartPreviousClose");

        BigDecimal change = null;
        BigDecimal changePct = null;
        if (price != null && prevClose != null) {
            change    = price.subtract(prevClose).stripTrailingZeros();
            changePct = prevClose.compareTo(BigDecimal.ZERO) != 0
                    ? change.divide(prevClose, 6, java.math.RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100)).stripTrailingZeros()
                    : null;
        }

        return StockQuoteDTO.builder()
                .symbol(textOrNull(meta, "symbol"))
                .companyName(textOrNull(meta, "longName") != null
                        ? textOrNull(meta, "longName") : textOrNull(meta, "shortName"))
                .exchange(textOrNull(meta, "fullExchangeName"))
                .currency(textOrNull(meta, "currency"))
                .currentPrice(price)
                .previousClose(prevClose)
                .open(decimalOrNull(meta, "regularMarketOpen"))
                .dayHigh(decimalOrNull(meta, "regularMarketDayHigh"))
                .dayLow(decimalOrNull(meta, "regularMarketDayLow"))
                .priceChange(change)
                .priceChangePercent(changePct)
                .volume(longOrNull(meta, "regularMarketVolume"))
                .avgVolume(null)
                .fiftyTwoWeekHigh(decimalOrNull(meta, "fiftyTwoWeekHigh"))
                .fiftyTwoWeekLow(decimalOrNull(meta, "fiftyTwoWeekLow"))
                .marketCap(null)
                .peRatio(null)
                .eps(null)
                .dividendYield(null)
                .build();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
        return headers;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull() && n.isNumber()) ? n.decimalValue() : null;
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull() && n.isNumber()) ? n.longValue() : null;
    }
}

