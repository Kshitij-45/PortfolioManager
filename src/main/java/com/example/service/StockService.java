package com.example.service;

import com.example.exception.InvalidSymbolException;
import com.example.exception.StockNotFoundException;
import com.example.exception.MarketDataParsingException;
import com.example.exception.MarketDataUnavailableException;
import com.example.dto.StockHistoryPointDTO;
import com.example.dto.StockQuoteDTO;
import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService extends BaseMarketDataService {

    private static final String YAHOO_HISTORY_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval={interval}&range={range}";

    public StockService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * Get a full quote for a single stock symbol (e.g. "AAPL", "TSLA").
     */
    public StockQuoteDTO getQuote(String symbol) {
        String upperSymbol = normalizeSymbol(symbol, "stock");
        JsonNode meta = fetchChartMeta(upperSymbol);
        if (meta == null || meta.isMissingNode()) {
            throw new StockNotFoundException(upperSymbol);
        }
        return mapMetaToDTO(meta);
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
        validateBatchSymbols(symbols, "stock");
        List<StockQuoteDTO> results = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                results.add(getQuote(symbol));
            } catch (StockNotFoundException | InvalidSymbolException ignored) {
                // skip invalid symbols in batch
            }
        }
        return results;
    }

    /**
     * Returns historical close prices for a symbol and range.
     */
    public List<StockHistoryPointDTO> getHistory(String symbol, String range) {
        String upperSymbol = normalizeSymbol(symbol, "stock");
        String yahooRange = normalizeHistoryRange(range);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    YAHOO_HISTORY_URL,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class,
                    upperSymbol,
                    "1d",
                    yahooRange);

            JsonNode root = response.getBody();
            if (root == null) {
                throw new MarketDataUnavailableException(upperSymbol, "Empty response from market data provider");
            }

            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) {
                throw new MarketDataUnavailableException(upperSymbol, "No history data returned by market data provider");
            }

            JsonNode series = result.get(0);
            JsonNode timestamps = series.path("timestamp");
            JsonNode closes = series.path("indicators").path("quote").path(0).path("close");

            if (!timestamps.isArray() || !closes.isArray()) {
                throw new MarketDataParsingException(upperSymbol, "Missing timestamp/close arrays in market data");
            }

            int pointCount = Math.min(timestamps.size(), closes.size());
            List<StockHistoryPointDTO> points = new ArrayList<>(pointCount);

            for (int i = 0; i < pointCount; i += 1) {
                JsonNode timestampNode = timestamps.get(i);
                JsonNode closeNode = closes.get(i);
                if (timestampNode == null || !timestampNode.isNumber() || closeNode == null || !closeNode.isNumber()) {
                    continue;
                }

                String date = Instant.ofEpochSecond(timestampNode.asLong())
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .toString();

                points.add(new StockHistoryPointDTO(date, closeNode.decimalValue()));
            }

            if (points.isEmpty()) {
                throw new MarketDataUnavailableException(upperSymbol, "No valid close-price points in market data");
            }

            return points;
        } catch (MarketDataUnavailableException | MarketDataParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new MarketDataUnavailableException(upperSymbol, e.getMessage());
        }
    }

    private String normalizeHistoryRange(String range) {
        String normalized = String.valueOf(range).trim().toLowerCase();
        if ("1w".equals(normalized)) {
            return "5d";
        }
        if ("1y".equals(normalized)) {
            return "1y";
        }
        return "1mo";
    }

    private StockQuoteDTO mapMetaToDTO(JsonNode meta) {
        BigDecimal prevClose = firstDecimal(meta, "chartPreviousClose", "regularMarketPreviousClose");
        BigDecimal price     = firstDecimal(meta, "regularMarketPrice", "regularMarketPreviousClose", "chartPreviousClose");
        BigDecimal change    = computeChange(price, prevClose);
        BigDecimal changePct = computeChangePercent(change, prevClose);

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
}
