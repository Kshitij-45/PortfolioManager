package com.example.service;

import com.example.dto.AssetRecommendationDTO;
import com.example.dto.IndicatorDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AIRecommendationEngine {

    private static final Logger log = LoggerFactory.getLogger(AIRecommendationEngine.class);
    private static final int MAX_REASON_LENGTH = 220;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean aiEnabled;
    private final String aiApiUrl;
    private final String aiModel;
    private final String aiApiKey;
    private final int maxScoreAdjustment;
    private final int maxCallsPerMinute;
    private final AtomicLong aiAttempts = new AtomicLong(0);
    private final AtomicLong aiSuccesses = new AtomicLong(0);
    private final AtomicLong aiThrottled = new AtomicLong(0);
    private final AtomicLong aiRateWindowMinute = new AtomicLong(System.currentTimeMillis() / 60000L);
    private final AtomicInteger aiRateWindowCount = new AtomicInteger(0);
    private volatile String lastAiError;

    public AIRecommendationEngine(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${recommendation.ai.enabled:false}") boolean aiEnabled,
            @Value("${recommendation.ai.api-url:https://api.openai.com/v1/chat/completions}") String aiApiUrl,
            @Value("${recommendation.ai.model:gpt-4o-mini}") String aiModel,
            @Value("${recommendation.ai.api-key:}") String aiApiKey,
            @Value("${recommendation.ai.max-score-adjustment:2}") int maxScoreAdjustment,
            @Value("${recommendation.ai.max-calls-per-minute:4}") int maxCallsPerMinute) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.aiEnabled = aiEnabled;
        this.aiApiUrl = aiApiUrl;
        this.aiModel = aiModel;
        this.aiApiKey = aiApiKey;
        this.maxScoreAdjustment = Math.max(0, Math.min(5, maxScoreAdjustment));
        this.maxCallsPerMinute = Math.max(1, maxCallsPerMinute);
    }

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

        AssetRecommendationDTO baseline = buildRecommendation(
                ticker, companyName, currentPrice, "Stock", score, 11, reasons, indicator.getDailyVolatility());
        return maybeEnhanceWithLlm(baseline, indicator);
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

        AssetRecommendationDTO baseline = buildRecommendation(
                ticker, companyName, currentPrice, "Crypto", score, 11, reasons, indicator.getDailyVolatility());
        return maybeEnhanceWithLlm(baseline, indicator);
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

        AssetRecommendationDTO baseline = buildRecommendation(
                ticker, companyName, currentPrice, "ETF/Fund", score, 10, reasons, indicator.getDailyVolatility());
        return maybeEnhanceWithLlm(baseline, indicator);
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

        AssetRecommendationDTO baseline = buildRecommendation(
                ticker, companyName, currentPrice, "Bond ETF", score, 10, reasons, indicator.getDailyVolatility());
        return maybeEnhanceWithLlm(baseline, indicator);
    }

    private AssetRecommendationDTO maybeEnhanceWithLlm(AssetRecommendationDTO baseline, IndicatorDTO indicator) {
        if (baseline == null || !aiEnabled) {
            return baseline;
        }

        if (!StringUtils.hasText(aiApiKey)) {
            log.debug("AI recommendation enabled but API key is not configured; using baseline rules.");
            lastAiError = "API key is missing";
            return baseline;
        }

        if (!tryAcquireAiCallSlot()) {
            aiThrottled.incrementAndGet();
            lastAiError = "AI calls throttled to stay within quota";
            return baseline;
        }

        try {
            aiAttempts.incrementAndGet();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiApiKey.trim());

            String requestBody = objectMapper.writeValueAsString(buildLlmRequestBody(baseline, indicator));
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(aiApiUrl, request, String.class);
            String llmContent = extractLlmContent(response.getBody());

            if (!StringUtils.hasText(llmContent)) {
                lastAiError = "LLM response content was empty";
                return baseline;
            }

            AssetRecommendationDTO enhanced = mergeLlmDecision(baseline, llmContent);
            if (enhanced != baseline) {
                aiSuccesses.incrementAndGet();
                lastAiError = null;
            }
            return enhanced;
        } catch (Exception e) {
            log.warn("LLM enhancement failed for {}: {}", baseline.getTicker(), e.getMessage());
            lastAiError = e.getClass().getSimpleName() + ": " + e.getMessage();
            return baseline;
        }
    }

    private JsonNode buildLlmRequestBody(AssetRecommendationDTO baseline, IndicatorDTO indicator) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", aiModel);
        payload.put("temperature", 0.2);

        if (isOpenAiCompatibleUrl(aiApiUrl)) {
            ObjectNode responseFormat = payload.putObject("response_format");
            responseFormat.put("type", "json_object");
        }

        ArrayNode messages = payload.putArray("messages");

        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content",
                "You are a financial recommendation ranker. Reply with ONLY JSON having keys: "
                        + "recommendation (STRONG_BUY|BUY|HOLD|AVOID), confidence (5-99), "
                        + "score_delta (integer between -" + maxScoreAdjustment + " and " + maxScoreAdjustment + "), "
                        + "reason (max 1 sentence, no markdown).");

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", buildLlmUserPrompt(baseline, indicator));
        return payload;
    }

    private String buildLlmUserPrompt(AssetRecommendationDTO baseline, IndicatorDTO indicator) {
        StringBuilder sb = new StringBuilder();
        sb.append("Asset type: ").append(safeText(baseline.getAssetType())).append('\n');
        sb.append("Ticker: ").append(safeText(baseline.getTicker())).append('\n');
        sb.append("Name: ").append(safeText(baseline.getCompanyName())).append('\n');
        sb.append("Current price: ").append(safeDecimal(baseline.getCurrentPrice())).append('\n');
        sb.append("Baseline recommendation: ").append(safeText(baseline.getRecommendation())).append('\n');
        sb.append("Baseline score: ").append(baseline.getScore()).append('\n');
        sb.append("Baseline confidence: ").append(baseline.getConfidence()).append('\n');
        sb.append("Risk level: ").append(safeText(baseline.getRiskLevel())).append('\n');
        sb.append("Indicators: ");

        if (indicator == null) {
            sb.append("not available");
            return sb.toString();
        }

        sb.append("RSI14=").append(safeDecimal(indicator.getRsi14())).append(", ");
        sb.append("SMA20=").append(safeDecimal(indicator.getSma20())).append(", ");
        sb.append("SMA50=").append(safeDecimal(indicator.getSma50())).append(", ");
        sb.append("EMA20=").append(safeDecimal(indicator.getEma20())).append(", ");
        sb.append("Return30d=").append(safeDecimal(indicator.getReturn30d())).append(", ");
        sb.append("DailyVolatility=").append(safeDecimal(indicator.getDailyVolatility())).append(", ");
        sb.append("MACDBullish=").append(indicator.isMacdBullish()).append(", ");
        sb.append("MomentumPositive=").append(indicator.isMomentumPositive()).append(", ");
        sb.append("IncreasingVolume=").append(indicator.isIncreasingVolume());
        return sb.toString();
    }

    private String extractLlmContent(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }

        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }

        JsonNode choices = parsed.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }

        JsonNode content = choices.get(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }

        return null;
    }

    private AssetRecommendationDTO mergeLlmDecision(AssetRecommendationDTO baseline, String llmJson) {
        try {
            JsonNode parsed = objectMapper.readTree(extractJsonPayload(llmJson));
            String recommendation = normalizeRecommendation(parsed.path("recommendation").asText(null));
            Integer confidence = boundedInt(parsed.get("confidence"), 5, 99);
            Integer scoreDelta = boundedInt(parsed.get("score_delta"), -maxScoreAdjustment, maxScoreAdjustment);
            String reason = sanitizeReason(parsed.path("reason").asText(null));

            int score = baseline.getScore();
            if (scoreDelta != null) {
                score = Math.max(0, score + scoreDelta);
            }

            List<String> mergedReasons = new ArrayList<>();
            if (baseline.getReasons() != null) {
                mergedReasons.addAll(baseline.getReasons());
            }
            if (StringUtils.hasText(reason)) {
                mergedReasons.add("AI insight: " + reason);
            }

            return AssetRecommendationDTO.builder()
                    .ticker(baseline.getTicker())
                    .companyName(baseline.getCompanyName())
                    .currentPrice(baseline.getCurrentPrice())
                    .assetType(baseline.getAssetType())
                    .recommendation(recommendation != null ? recommendation : baseline.getRecommendation())
                    .score(score)
                    .confidence(confidence != null ? confidence : baseline.getConfidence())
                    .riskLevel(baseline.getRiskLevel())
                    .reasons(mergedReasons)
                    .build();
        } catch (Exception e) {
            lastAiError = "JSON parse failure: " + e.getMessage();
            return baseline;
        }
    }

    public Map<String, Object> getAiStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", aiEnabled);
        status.put("apiUrl", aiApiUrl);
        status.put("model", aiModel);
        status.put("keyConfigured", StringUtils.hasText(aiApiKey));
        status.put("maxCallsPerMinute", maxCallsPerMinute);
        status.put("attempts", aiAttempts.get());
        status.put("successes", aiSuccesses.get());
        status.put("throttled", aiThrottled.get());
        status.put("lastError", lastAiError);
        return status;
    }

    private boolean tryAcquireAiCallSlot() {
        long currentMinute = System.currentTimeMillis() / 60000L;
        long trackedMinute = aiRateWindowMinute.get();

        if (currentMinute != trackedMinute && aiRateWindowMinute.compareAndSet(trackedMinute, currentMinute)) {
            aiRateWindowCount.set(0);
        }

        while (true) {
            int current = aiRateWindowCount.get();
            if (current >= maxCallsPerMinute) {
                return false;
            }

            if (aiRateWindowCount.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private boolean isOpenAiCompatibleUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }

        String normalized = url.toLowerCase();
        return normalized.contains("openai.com") || normalized.contains("/openai/");
    }

    private String extractJsonPayload(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return rawContent;
        }

        String content = rawContent.trim();
        if (content.startsWith("```") && content.endsWith("```")) {
            int firstNewLine = content.indexOf('\n');
            if (firstNewLine >= 0) {
                content = content.substring(firstNewLine + 1, content.length() - 3).trim();
            }
        }

        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        return content;
    }

    private Integer boundedInt(JsonNode node, int min, int max) {
        if (node == null || node.isNull()) {
            return null;
        }

        Integer value = null;
        if (node.isInt() || node.isLong()) {
            value = node.asInt();
        } else if (node.isTextual()) {
            try {
                value = Integer.parseInt(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        if (value == null) {
            return null;
        }

        return Math.max(min, Math.min(max, value));
    }

    private String normalizeRecommendation(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "STRONG_BUY":
            case "BUY":
            case "HOLD":
            case "AVOID":
                return normalized;
            default:
                return null;
        }
    }

    private String sanitizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }

        String cleaned = reason.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > MAX_REASON_LENGTH) {
            cleaned = cleaned.substring(0, MAX_REASON_LENGTH);
        }
        return cleaned;
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value : "n/a";
    }

    private String safeDecimal(BigDecimal value) {
        return value == null ? "n/a" : value.toPlainString();
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
