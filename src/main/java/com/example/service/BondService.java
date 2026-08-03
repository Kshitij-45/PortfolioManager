package com.example.service;

import com.example.exception.AssetNotFoundException;
import com.example.exception.InvalidSymbolException;
import com.example.dto.BondQuoteDTO;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BondService extends BaseMarketDataService {

    public BondService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * Get a full quote for a bond or bond ETF symbol.
     * Examples: TLT (20yr Treasury ETF), AGG (Aggregate Bond ETF),
     *           ^TNX (10-yr yield), ^TYX (30-yr yield), BND, IEF
     */
    public BondQuoteDTO getQuote(String symbol) {
        String upperSymbol = normalizeSymbol(symbol, "bond");
        JsonNode meta = fetchChartMeta(upperSymbol);
        if (meta == null || meta.isMissingNode()) {
            throw new AssetNotFoundException("Bond", upperSymbol);
        }
        return mapToDTO(meta);
    }

    /**
     * Get current price only for a bond symbol.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        return getQuote(symbol).getCurrentPrice();
    }

    /**
     * Get quotes for multiple bond symbols.
     */
    public List<BondQuoteDTO> getQuotes(List<String> symbols) {
        validateBatchSymbols(symbols, "bond");
        List<BondQuoteDTO> results = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                results.add(getQuote(symbol));
            } catch (AssetNotFoundException | InvalidSymbolException ignored) {
                // skip invalid symbols in batch
            }
        }
        return results;
    }

    private BondQuoteDTO mapToDTO(JsonNode meta) {
        BigDecimal price     = decimalOrNull(meta, "regularMarketPrice");
        BigDecimal prevClose = decimalOrNull(meta, "chartPreviousClose");
        BigDecimal change    = computeChange(price, prevClose);
        BigDecimal changePct = computeChangePercent(change, prevClose);

        return BondQuoteDTO.builder()
                .symbol(textOrNull(meta, "symbol"))
                .name(textOrNull(meta, "longName") != null
                        ? textOrNull(meta, "longName") : textOrNull(meta, "shortName"))
                .exchange(textOrNull(meta, "fullExchangeName"))
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
