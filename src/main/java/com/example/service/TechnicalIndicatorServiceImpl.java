package com.example.service;

import com.example.dto.IndicatorDTO;
import com.example.dto.MarketCandleDTO;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class TechnicalIndicatorServiceImpl implements TechnicalIndicatorService {

    private static final int DEFAULT_SCALE = 4;

    @Override
    public IndicatorDTO calculateIndicators(List<MarketCandleDTO> candles) {
        List<MarketCandleDTO> sanitized = sanitize(candles);
        if (sanitized.size() < 30) {
            return IndicatorDTO.builder().build();
        }

        BarSeries series = toSeries(sanitized);
        int end = series.getEndIndex();

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        EMAIndicator ema20 = new EMAIndicator(closePrice, 20);
        RSIIndicator rsi14 = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator signal = new EMAIndicator(macd, 9);

        SMAIndicator bbSma = new SMAIndicator(closePrice, 20);
        BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(bbSma);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMiddle, stdDev);
        BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMiddle, stdDev);

        BigDecimal sma20Val = numToBigDecimal(sma20.getValue(end));
        BigDecimal sma50Val = numToBigDecimal(sma50.getValue(end));
        BigDecimal ema20Val = numToBigDecimal(ema20.getValue(end));
        BigDecimal rsi14Val = numToBigDecimal(rsi14.getValue(end));
        BigDecimal macdVal = numToBigDecimal(macd.getValue(end));
        BigDecimal signalVal = numToBigDecimal(signal.getValue(end));
        BigDecimal bbUpperVal = numToBigDecimal(bbUpper.getValue(end));
        BigDecimal bbMiddleVal = numToBigDecimal(bbMiddle.getValue(end));
        BigDecimal bbLowerVal = numToBigDecimal(bbLower.getValue(end));

        BigDecimal latestClose = sanitized.get(sanitized.size() - 1).getClose();
        BigDecimal latestVolume = BigDecimal.valueOf(safeVolume(sanitized.get(sanitized.size() - 1)));

        BigDecimal return30d = calculateReturn(sanitized, 30);
        BigDecimal averageVolume = calculateAverageVolume(sanitized, 20);
        BigDecimal dailyVolatility = calculateDailyVolatility(sanitized, 20);

        boolean macdBullish = macdVal != null && signalVal != null && macdVal.compareTo(signalVal) > 0;
        boolean increasingVolume = averageVolume != null
                && latestVolume.compareTo(BigDecimal.ZERO) > 0
                && latestVolume.compareTo(averageVolume) > 0;
        boolean momentumPositive = sma20Val != null && sma50Val != null && sma20Val.compareTo(sma50Val) > 0;

        return IndicatorDTO.builder()
                .sma20(scale(sma20Val))
                .sma50(scale(sma50Val))
                .ema20(scale(ema20Val))
                .rsi14(scale(rsi14Val))
                .macd(scale(macdVal))
                .signalLine(scale(signalVal))
                .bollingerUpper(scale(bbUpperVal))
                .bollingerMiddle(scale(bbMiddleVal))
                .bollingerLower(scale(bbLowerVal))
                .return30d(scale(return30d))
                .averageVolume(scale(averageVolume))
                .dailyVolatility(scale(dailyVolatility))
                .latestClose(scale(latestClose))
                .latestVolume(scale(latestVolume))
                .macdBullish(macdBullish)
                .increasingVolume(increasingVolume)
                .momentumPositive(momentumPositive)
                .build();
    }

    private List<MarketCandleDTO> sanitize(List<MarketCandleDTO> candles) {
        List<MarketCandleDTO> sanitized = new ArrayList<>();
        if (candles == null) {
            return sanitized;
        }

        for (MarketCandleDTO candle : candles) {
            if (candle == null || candle.getDate() == null || candle.getClose() == null
                    || candle.getClose().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal close = candle.getClose();
            sanitized.add(MarketCandleDTO.builder()
                    .date(candle.getDate())
                    .open(defaultPrice(candle.getOpen(), close))
                    .high(defaultPrice(candle.getHigh(), close))
                    .low(defaultPrice(candle.getLow(), close))
                    .close(close)
                    .volume(safeVolume(candle))
                    .build());
        }

        return sanitized;
    }

    private BarSeries toSeries(List<MarketCandleDTO> candles) {
        BarSeries series = new BaseBarSeriesBuilder().withName("recommendation-series").build();
        for (MarketCandleDTO candle : candles) {
            LocalDate date = candle.getDate();
            ZonedDateTime endTime = date.atStartOfDay(ZoneOffset.UTC).plusHours(23);

                Bar bar = series.barBuilder()
                    .timePeriod(Duration.ofDays(1))
                    .endTime(endTime.toInstant())
                    .openPrice(candle.getOpen().doubleValue())
                    .highPrice(candle.getHigh().doubleValue())
                    .lowPrice(candle.getLow().doubleValue())
                    .closePrice(candle.getClose().doubleValue())
                    .volume(candle.getVolume().doubleValue())
                    .build();

            series.addBar(bar);
        }
        return series;
    }

    private BigDecimal calculateReturn(List<MarketCandleDTO> candles, int days) {
        if (candles.size() < 2) {
            return BigDecimal.ZERO;
        }

        int startIndex = Math.max(0, candles.size() - days - 1);
        BigDecimal start = candles.get(startIndex).getClose();
        BigDecimal end = candles.get(candles.size() - 1).getClose();
        if (start == null || start.compareTo(BigDecimal.ZERO) == 0 || end == null) {
            return BigDecimal.ZERO;
        }

        return end.subtract(start)
                .divide(start, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateAverageVolume(List<MarketCandleDTO> candles, int lookback) {
        if (candles.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int start = Math.max(0, candles.size() - lookback);
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        for (int i = start; i < candles.size(); i += 1) {
            total = total.add(BigDecimal.valueOf(safeVolume(candles.get(i))));
            count += 1;
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDailyVolatility(List<MarketCandleDTO> candles, int lookback) {
        if (candles.size() < 3) {
            return BigDecimal.ZERO;
        }

        int start = Math.max(1, candles.size() - lookback);
        List<BigDecimal> returns = new ArrayList<>();

        for (int i = start; i < candles.size(); i += 1) {
            BigDecimal previous = candles.get(i - 1).getClose();
            BigDecimal current = candles.get(i).getClose();
            if (previous == null || current == null || previous.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal dailyReturn = current.subtract(previous)
                    .divide(previous, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            returns.add(dailyReturn);
        }

        if (returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 8, RoundingMode.HALF_UP);

        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal r : returns) {
            BigDecimal diff = r.subtract(mean);
            variance = variance.add(diff.multiply(diff));
        }

        variance = variance.divide(BigDecimal.valueOf(returns.size() - 1), 8, RoundingMode.HALF_UP);
        double stddev = Math.sqrt(variance.doubleValue());

        return BigDecimal.valueOf(stddev);
    }

    private BigDecimal numToBigDecimal(org.ta4j.core.num.Num value) {
        if (value == null || value.isNaN()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value.doubleValue());
    }

    private BigDecimal defaultPrice(BigDecimal value, BigDecimal fallback) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return fallback;
        }
        return value;
    }

    private Long safeVolume(MarketCandleDTO candle) {
        if (candle == null || candle.getVolume() == null || candle.getVolume() < 0) {
            return 0L;
        }
        return candle.getVolume();
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }
}
