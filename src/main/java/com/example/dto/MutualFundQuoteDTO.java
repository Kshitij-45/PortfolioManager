package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MutualFundQuoteDTO {

    private String symbol;           // e.g. "VFINX", "FXAIX"
    private String name;             // fund full name
    private String currency;

    private BigDecimal nav;          // Net Asset Value (current price)
    private BigDecimal previousNav;
    private BigDecimal navChange;
    private BigDecimal navChangePercent;

    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;
}
