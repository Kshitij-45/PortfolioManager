package com.example.service;

import com.example.dto.MarketCandleDTO;
import com.example.exception.AssetNotFoundException;
import com.example.exception.InvalidSymbolException;
import com.example.dto.CryptoQuoteDTO;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CryptoService extends BaseMarketDataService {

    public CryptoService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * Get a full quote for a crypto symbol.
     * Use Yahoo Finance format: BTC-USD, ETH-USD, SOL-USD, BNB-USD, etc.
     */
    public CryptoQuoteDTO getQuote(String symbol) {
        String upperSymbol = normalizeCryptoSymbol(symbol);
        JsonNode meta = fetchChartMeta(upperSymbol);
        if (meta == null || meta.isMissingNode()) {
            throw new AssetNotFoundException("Crypto", upperSymbol);
        }
        return mapToDTO(meta);
    }

    /**
     * Get current price only for a crypto symbol.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        return getQuote(symbol).getCurrentPrice();
    }

    /**
     * Get quotes for multiple crypto symbols.
     */
    public List<CryptoQuoteDTO> getQuotes(List<String> symbols) {
        validateBatchSymbols(symbols, "crypto");
        List<CryptoQuoteDTO> results = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                results.add(getQuote(symbol));
            } catch (AssetNotFoundException | InvalidSymbolException ignored) {
                // skip invalid symbols in batch
            }
        }
        return results;
    }

    /**
     * Returns OHLCV data used by the recommendation engine.
     */
    public List<MarketCandleDTO> getDailyHistory(String symbol, String range) {
        String upperSymbol = normalizeCryptoSymbol(symbol);
        return fetchDailyHistory(upperSymbol, range);
    }

    private String normalizeCryptoSymbol(String symbol) {
        String normalized = normalizeSymbol(symbol, "crypto");
        return normalized.contains("-") ? normalized : normalized + "-USD";
    }

    private CryptoQuoteDTO mapToDTO(JsonNode meta) {
        BigDecimal prevClose = firstDecimal(meta, "chartPreviousClose", "regularMarketPreviousClose");
        BigDecimal price     = firstDecimal(meta, "regularMarketPrice", "regularMarketPreviousClose", "chartPreviousClose");
        BigDecimal change    = computeChange(price, prevClose);
        BigDecimal changePct = computeChangePercent(change, prevClose);

        return CryptoQuoteDTO.builder()
                .symbol(textOrNull(meta, "symbol"))
                .name(textOrNull(meta, "longName") != null
                        ? textOrNull(meta, "longName") : textOrNull(meta, "shortName"))
                .currency(textOrNull(meta, "currency"))
                .currentPrice(price)
                .previousClose(prevClose)
                .priceChange(change)
                .priceChangePercent(changePct)
                .dayHigh(decimalOrNull(meta, "regularMarketDayHigh"))
                .dayLow(decimalOrNull(meta, "regularMarketDayLow"))
                .volume(longOrNull(meta, "regularMarketVolume"))
                .fiftyTwoWeekHigh(decimalOrNull(meta, "fiftyTwoWeekHigh"))
                .fiftyTwoWeekLow(decimalOrNull(meta, "fiftyTwoWeekLow"))
                .build();
    }
}
