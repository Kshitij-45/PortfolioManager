package com.example.service;

import com.example.exception.InvalidSymbolException;
import com.example.exception.StockNotFoundException;
import com.example.dto.StockQuoteDTO;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService extends BaseMarketDataService {

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

    private StockQuoteDTO mapMetaToDTO(JsonNode meta) {
        BigDecimal price     = decimalOrNull(meta, "regularMarketPrice");
        BigDecimal prevClose = decimalOrNull(meta, "chartPreviousClose");
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
