package com.example.service;

import com.example.Exception.AssetNotFoundException;
import com.example.dto.MutualFundQuoteDTO;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MutualFundService extends BaseMarketDataService {

    public MutualFundService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * Get a full quote for a mutual fund symbol.
     * Examples: VFINX (Vanguard 500), FXAIX (Fidelity 500), VTSAX, FSKAX
     */
    public MutualFundQuoteDTO getQuote(String symbol) {
        String upperSymbol = symbol.trim().toUpperCase();
        JsonNode meta = fetchChartMeta(upperSymbol);
        if (meta == null || meta.isMissingNode()) {
            throw new AssetNotFoundException("Mutual Fund", upperSymbol);
        }
        return mapToDTO(meta);
    }

    /**
     * Get current NAV only for a mutual fund symbol.
     */
    public BigDecimal getCurrentNav(String symbol) {
        return getQuote(symbol).getNav();
    }

    /**
     * Get quotes for multiple mutual fund symbols.
     */
    public List<MutualFundQuoteDTO> getQuotes(List<String> symbols) {
        List<MutualFundQuoteDTO> results = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                results.add(getQuote(symbol));
            } catch (AssetNotFoundException ignored) {
                // skip invalid symbols in batch
            }
        }
        return results;
    }

    private MutualFundQuoteDTO mapToDTO(JsonNode meta) {
        BigDecimal nav     = decimalOrNull(meta, "regularMarketPrice");
        BigDecimal prevNav = decimalOrNull(meta, "chartPreviousClose");
        BigDecimal change  = computeChange(nav, prevNav);
        BigDecimal pct     = computeChangePercent(change, prevNav);

        return MutualFundQuoteDTO.builder()
                .symbol(textOrNull(meta, "symbol"))
                .name(textOrNull(meta, "longName") != null
                        ? textOrNull(meta, "longName") : textOrNull(meta, "shortName"))
                .currency(textOrNull(meta, "currency"))
                .nav(nav)
                .previousNav(prevNav)
                .navChange(change)
                .navChangePercent(pct)
                .fiftyTwoWeekHigh(decimalOrNull(meta, "fiftyTwoWeekHigh"))
                .fiftyTwoWeekLow(decimalOrNull(meta, "fiftyTwoWeekLow"))
                .build();
    }
}
