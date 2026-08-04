package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IndicatorDTO {

    private BigDecimal sma20;
    private BigDecimal sma50;
    private BigDecimal ema20;
    private BigDecimal rsi14;
    private BigDecimal macd;
    private BigDecimal signalLine;
    private BigDecimal bollingerUpper;
    private BigDecimal bollingerMiddle;
    private BigDecimal bollingerLower;
    private BigDecimal return30d;
    private BigDecimal averageVolume;
    private BigDecimal dailyVolatility;

    private BigDecimal latestClose;
    private BigDecimal latestVolume;

    private boolean macdBullish;
    private boolean increasingVolume;
    private boolean momentumPositive;
}
