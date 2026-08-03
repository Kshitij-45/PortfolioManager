package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BondQuoteDTO {

    private String symbol;           // e.g. "^TNX", "TLT", "AGG"
    private String name;             // e.g. "iShares 20+ Year Treasury Bond ETF"
    private String exchange;
    private String currency;

    private BigDecimal currentPrice; // bond price or yield (depends on symbol)
    private BigDecimal previousClose;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;

    private BigDecimal dayHigh;
    private BigDecimal dayLow;

    private Long volume;

    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;
}
