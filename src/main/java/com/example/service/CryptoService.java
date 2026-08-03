package com.example.service;

import com.example.Exception.AssetNotFoundException;
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
        String upperSymbol = symbol.trim().toUpperCase();
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
        List<CryptoQuoteDTO> results = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                results.add(getQuote(symbol));
            } catch (AssetNotFoundException ignored) {
                // skip invalid symbols in batch
            }
        }
        return results;
    }

    private CryptoQuoteDTO mapToDTO(JsonNode meta) {
        BigDecimal price     = decimalOrNull(meta, "regularMarketPrice");
        BigDecimal prevClose = decimalOrNull(meta, "chartPreviousClose");
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
