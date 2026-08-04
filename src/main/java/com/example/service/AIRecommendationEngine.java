package com.example.service;

import com.example.dto.AssetRecommendationDTO;
import com.example.dto.IndicatorDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class AIRecommendationEngine {

    public AssetRecommendationDTO evaluateStock(String ticker, String companyName, BigDecimal currentPrice, IndicatorDTO indicator) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (gt(indicator.getSma20(), indicator.getSma50())) {
            score += 2;
            reasons.add("SMA20 is above SMA50");
        }

        if (gt(indicator.getEma20(), indicator.getSma50())) {
            score += 1;
            reasons.add("EMA20 is above SMA50");
        }

        if (between(indicator.getRsi14(), 35, 70)) {
            score += 2;
            reasons.add("RSI is in a healthy range");
        }

        if (indicator.isMacdBullish()) {
            score += 2;
            reasons.add("Bullish MACD crossover");
        }

        if (positive(indicator.getReturn30d())) {
            score += 2;
            reasons.add("Positive 30-day return");
        }

        if (indicator.isIncreasingVolume()) {
            score += 1;
            reasons.add("Volume is above 20-day average");
        }

        if (lowVolatility(indicator.getDailyVolatility(), 2.0)) {
            score += 1;
            reasons.add("Low recent daily volatility");
        }

        return buildRecommendation(ticker, companyName, currentPrice, "Stock", score, 11, reasons, indicator.getDailyVolatility());
    }

    public AssetRecommendationDTO evaluateCrypto(String ticker, String companyName, BigDecimal currentPrice, IndicatorDTO indicator) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (indicator.isMomentumPositive() && positive(indicator.getReturn30d())) {
            score += 3;
            reasons.add("Momentum remains positive");
        }

        if (between(indicator.getRsi14(), 40, 75)) {
            score += 2;
            reasons.add("RSI supports continuation");
        }

        if (indicator.isMacdBullish()) {
            score += 2;
            reasons.add("MACD is bullish");
        }

        if (positive(indicator.getReturn30d())) {
            score += 2;
            reasons.add("Positive 30-day return");
        }

        if (indicator.isIncreasingVolume()) {
            score += 1;
            reasons.add("Rising participation volume");
        }

        if (lowVolatility(indicator.getDailyVolatility(), 3.5)) {
            score += 1;
            reasons.add("Volatility remains manageable");
        }

        return buildRecommendation(ticker, companyName, currentPrice, "Crypto", score, 11, reasons, indicator.getDailyVolatility());
    }

    public AssetRecommendationDTO evaluateFund(String ticker, String companyName, BigDecimal currentPrice, IndicatorDTO indicator) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (gt(indicator.getSma20(), indicator.getSma50()) && gt(indicator.getEma20(), indicator.getSma50())) {
            score += 3;
            reasons.add("Uptrend confirmed by moving averages");
        }

        if (between(indicator.getRsi14(), 40, 70)) {
            score += 2;
            reasons.add("RSI trend is constructive");
        }

        if (positive(indicator.getReturn30d())) {
            score += 3;
            reasons.add("Strong 30-day return profile");
        }

        if (lowVolatility(indicator.getDailyVolatility(), 1.8)) {
            score += 2;
            reasons.add("Low volatility supports stability");
        }

        return buildRecommendation(ticker, companyName, currentPrice, "ETF/Fund", score, 10, reasons, indicator.getDailyVolatility());
    }

    public AssetRecommendationDTO evaluateBond(String ticker, String companyName, BigDecimal currentPrice, IndicatorDTO indicator) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (gt(indicator.getSma20(), indicator.getSma50())) {
            score += 3;
            reasons.add("Stable trend above longer average");
        }

        if (lowVolatility(indicator.getDailyVolatility(), 1.2)) {
            score += 3;
            reasons.add("Low daily volatility");
        }

        if (positive(indicator.getReturn30d())) {
            score += 2;
            reasons.add("Positive monthly return");
        }

        if (gt(indicator.getEma20(), indicator.getSma50())) {
            score += 2;
            reasons.add("Short-term moving average confirmation");
        }

        return buildRecommendation(ticker, companyName, currentPrice, "Bond ETF", score, 10, reasons, indicator.getDailyVolatility());
    }

    private AssetRecommendationDTO buildRecommendation(
            String ticker,
            String companyName,
            BigDecimal currentPrice,
            String assetType,
            int score,
            int maxScore,
            List<String> reasons,
            BigDecimal volatility) {

        String recommendation = recommendationLabel(score, maxScore);
        int confidence = Math.max(5, Math.min(99, (int) Math.round((score * 100.0) / maxScore)));
        String riskLevel = classifyRisk(assetType, volatility);

        if (reasons.isEmpty()) {
            reasons.add("Limited signal strength from current 6-month market data");
        }

        return AssetRecommendationDTO.builder()
                .ticker(ticker)
                .companyName((companyName == null || companyName.isBlank()) ? ticker : companyName)
                .currentPrice(currentPrice)
                .assetType(assetType)
                .recommendation(recommendation)
                .score(score)
                .confidence(confidence)
                .riskLevel(riskLevel)
                .reasons(reasons)
                .build();
    }

    private String recommendationLabel(int score, int maxScore) {
        // Practical bands based on signal ratio so recommendations are not unrealistically strict.
        double ratio = maxScore <= 0 ? 0.0 : (score * 1.0) / maxScore;

        if (ratio >= 0.72) return "STRONG_BUY";
        if (ratio >= 0.54) return "BUY";
        if (ratio >= 0.36) return "HOLD";
        return "AVOID";
    }

    private String classifyRisk(String assetType, BigDecimal dailyVolatility) {
        double vol = dailyVolatility == null ? 0.0 : dailyVolatility.doubleValue();

        if ("Crypto".equalsIgnoreCase(assetType)) {
            if (vol < 2.0) return "Medium";
            return "High";
        }

        if ("Bond ETF".equalsIgnoreCase(assetType)) {
            if (vol < 1.2) return "Low";
            if (vol < 2.0) return "Medium";
            return "High";
        }

        if (vol < 1.5) return "Low";
        if (vol < 3.0) return "Medium";
        return "High";
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean gt(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) > 0;
    }

    private boolean between(BigDecimal value, double min, double max) {
        if (value == null) {
            return false;
        }

        double v = value.doubleValue();
        return v >= min && v <= max;
    }

    private boolean lowVolatility(BigDecimal value, double threshold) {
        if (value == null) {
            return false;
        }

        return value.doubleValue() <= threshold;
    }
}
