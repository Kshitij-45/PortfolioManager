package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CryptoQuoteDTO {

    private String symbol;           // e.g. "BTC-USD"
    private String name;             // e.g. "Bitcoin USD"
    private String currency;         // e.g. "USD"

    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;

    private BigDecimal dayHigh;
    private BigDecimal dayLow;

    private Long volume;

    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;
}
