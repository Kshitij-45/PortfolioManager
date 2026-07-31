package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StockQuoteDTO {

    private String symbol;
    private String companyName;
    private String exchange;
    private String currency;

    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal open;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;

    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;

    private Long volume;
    private Long avgVolume;

    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;

    private BigDecimal marketCap;
    private BigDecimal peRatio;
    private BigDecimal eps;
    private BigDecimal dividendYield;
}
