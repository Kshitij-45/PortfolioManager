package com.example.service;

import com.example.dto.AssetRecommendationDTO;
import com.example.dto.IndicatorDTO;
import com.example.dto.MarketCandleDTO;
import com.example.dto.RecommendationDTO;
import com.example.exception.RecommendationProcessingException;
import org.springframework.cache.annotation.CacheEvict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private static final Map<String, String> ASSET_NAME_MAP = buildAssetNameMap();
    private static final long SYMBOL_TIMEOUT_SECONDS = 8;
    private static final int LABEL_MIN_COUNT = 3;

    private final WatchlistService watchlistService;
    private final StockService stockService;
    private final CryptoService cryptoService;
    private final MutualFundService mutualFundService;
    private final BondService bondService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final AIRecommendationEngine recommendationEngine;
    private final Executor recommendationExecutor;

    public RecommendationServiceImpl(
            WatchlistService watchlistService,
            StockService stockService,
            CryptoService cryptoService,
            MutualFundService mutualFundService,
            BondService bondService,
            TechnicalIndicatorService technicalIndicatorService,
            AIRecommendationEngine recommendationEngine,
            Executor recommendationExecutor) {
        this.watchlistService = watchlistService;
        this.stockService = stockService;
        this.cryptoService = cryptoService;
        this.mutualFundService = mutualFundService;
        this.bondService = bondService;
        this.technicalIndicatorService = technicalIndicatorService;
        this.recommendationEngine = recommendationEngine;
        this.recommendationExecutor = recommendationExecutor;
    }

    @Override
    @Cacheable(value = "recommendations", key = "'all'", cacheManager = "recommendationCacheManager")
    public RecommendationDTO getAllRecommendations() {
        try {
            // Avoid nested async execution on the same executor.
            // Each category already parallelizes symbol processing internally.
            List<AssetRecommendationDTO> stocks = getStockRecommendations();
            List<AssetRecommendationDTO> crypto = getCryptoRecommendations();
            List<AssetRecommendationDTO> funds = getFundRecommendations();
            List<AssetRecommendationDTO> bonds = getBondRecommendations();

            return RecommendationDTO.builder()
                .stocks(stocks)
                .crypto(crypto)
                .funds(funds)
                .bonds(bonds)
                .build();
        } catch (Exception e) {
            throw new RecommendationProcessingException("Unable to compute recommendation set", e);
        }
    }

    @Override
    @CacheEvict(value = "recommendations", allEntries = true, cacheManager = "recommendationCacheManager")
    public RecommendationDTO refreshAllRecommendations() {
        return getAllRecommendations();
    }

    @Override
    @Cacheable(value = "recommendations", key = "'stocks'", cacheManager = "recommendationCacheManager")
    public List<AssetRecommendationDTO> getStockRecommendations() {
        List<AssetRecommendationDTO> ranked = watchlistService.getStockWatchlist().stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> buildStockRecommendation(symbol), recommendationExecutor))
                .collect(Collectors.toList())
                .stream()
                .map(this::resolveSafely)
                .filter(item -> item != null)
                .sorted(recommendationSorter())
                .collect(Collectors.toList());

        return rebalanceLabelsForDisplay(ranked, LABEL_MIN_COUNT);
    }

    @Override
    @Cacheable(value = "recommendations", key = "'crypto'", cacheManager = "recommendationCacheManager")
    public List<AssetRecommendationDTO> getCryptoRecommendations() {
        List<AssetRecommendationDTO> ranked = watchlistService.getCryptoWatchlist().stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> buildCryptoRecommendation(symbol), recommendationExecutor))
                .collect(Collectors.toList())
                .stream()
                .map(this::resolveSafely)
                .filter(item -> item != null)
                .sorted(recommendationSorter())
                .collect(Collectors.toList());

        return rebalanceLabelsForDisplay(ranked, LABEL_MIN_COUNT);
    }

    @Override
    @Cacheable(value = "recommendations", key = "'funds'", cacheManager = "recommendationCacheManager")
    public List<AssetRecommendationDTO> getFundRecommendations() {
        List<AssetRecommendationDTO> ranked = watchlistService.getFundWatchlist().stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> buildFundRecommendation(symbol), recommendationExecutor))
                .collect(Collectors.toList())
                .stream()
                .map(this::resolveSafely)
                .filter(item -> item != null)
                .sorted(recommendationSorter())
                .collect(Collectors.toList());

        return rebalanceLabelsForDisplay(ranked, LABEL_MIN_COUNT);
    }

    @Override
    @Cacheable(value = "recommendations", key = "'bonds'", cacheManager = "recommendationCacheManager")
    public List<AssetRecommendationDTO> getBondRecommendations() {
        List<AssetRecommendationDTO> ranked = watchlistService.getBondWatchlist().stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> buildBondRecommendation(symbol), recommendationExecutor))
                .collect(Collectors.toList())
                .stream()
                .map(this::resolveSafely)
                .filter(item -> item != null)
                .sorted(recommendationSorter())
                .collect(Collectors.toList());

        return rebalanceLabelsForDisplay(ranked, LABEL_MIN_COUNT);
    }

    private AssetRecommendationDTO buildStockRecommendation(String symbol) {
        try {
            List<MarketCandleDTO> history = stockService.getDailyHistory(symbol, "6mo");
            IndicatorDTO indicator = technicalIndicatorService.calculateIndicators(history);
            BigDecimal currentPrice = latestClose(history);
            return recommendationEngine.evaluateStock(symbol,
                    fallbackName(symbolToName(symbol), symbol),
                    fallbackPrice(currentPrice),
                    indicator);
        } catch (Exception e) {
            log.warn("Stock recommendation failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private AssetRecommendationDTO buildCryptoRecommendation(String symbol) {
        try {
            List<MarketCandleDTO> history = cryptoService.getDailyHistory(symbol, "6mo");
            IndicatorDTO indicator = technicalIndicatorService.calculateIndicators(history);
            BigDecimal currentPrice = latestClose(history);
            return recommendationEngine.evaluateCrypto(symbol,
                    fallbackName(symbolToName(symbol), symbol),
                    fallbackPrice(currentPrice),
                    indicator);
        } catch (Exception e) {
            log.warn("Crypto recommendation failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private AssetRecommendationDTO buildFundRecommendation(String symbol) {
        try {
            List<MarketCandleDTO> history = mutualFundService.getDailyHistory(symbol, "6mo");
            IndicatorDTO indicator = technicalIndicatorService.calculateIndicators(history);
            BigDecimal currentPrice = latestClose(history);
            return recommendationEngine.evaluateFund(symbol,
                    fallbackName(symbolToName(symbol), symbol),
                    fallbackPrice(currentPrice),
                    indicator);
        } catch (Exception e) {
            log.warn("Fund recommendation failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private AssetRecommendationDTO buildBondRecommendation(String symbol) {
        try {
            List<MarketCandleDTO> history = bondService.getDailyHistory(symbol, "6mo");
            IndicatorDTO indicator = technicalIndicatorService.calculateIndicators(history);
            BigDecimal currentPrice = latestClose(history);
            return recommendationEngine.evaluateBond(symbol,
                    fallbackName(symbolToName(symbol), symbol),
                    fallbackPrice(currentPrice),
                    indicator);
        } catch (Exception e) {
            log.warn("Bond recommendation failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private AssetRecommendationDTO resolveSafely(CompletableFuture<AssetRecommendationDTO> future) {
        try {
            return future.get(SYMBOL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Recommendation task timed out after {} seconds", SYMBOL_TIMEOUT_SECONDS);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Comparator<AssetRecommendationDTO> recommendationSorter() {
        return Comparator
                .comparingInt(AssetRecommendationDTO::getScore).reversed()
                .thenComparingInt(AssetRecommendationDTO::getConfidence).reversed()
                .thenComparing(AssetRecommendationDTO::getCurrentPrice,
                        Comparator.nullsLast(Comparator.comparing(BigDecimal::doubleValue).reversed()));
    }

    private String fallbackName(String name, String symbol) {
        if (name == null || name.isBlank()) {
            return symbol;
        }
        return name;
    }

    private BigDecimal fallbackPrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private BigDecimal latestClose(List<MarketCandleDTO> history) {
        if (history == null || history.isEmpty()) {
            return BigDecimal.ZERO;
        }

        MarketCandleDTO last = history.get(history.size() - 1);
        if (last == null || last.getClose() == null) {
            return BigDecimal.ZERO;
        }

        return last.getClose();
    }

    private String symbolToName(String symbol) {
        if (symbol == null) {
            return null;
        }
        return ASSET_NAME_MAP.get(symbol.toUpperCase());
    }

    private List<AssetRecommendationDTO> rebalanceLabelsForDisplay(List<AssetRecommendationDTO> ranked, int minPerBucket) {
        if (ranked == null || ranked.isEmpty()) {
            return List.of();
        }

        // To guarantee at least N in each of STRONG_BUY, BUY, HOLD, AVOID,
        // we need at least 4*N recommendations.
        int required = minPerBucket * 4;
        if (ranked.size() < required) {
            return ranked;
        }

        List<AssetRecommendationDTO> adjusted = new ArrayList<>(ranked);
        int size = adjusted.size();

        for (int i = 0; i < size; i += 1) {
            AssetRecommendationDTO dto = adjusted.get(i);
            if (dto == null) {
                continue;
            }

            String label;
            if (i < minPerBucket) {
                label = "STRONG_BUY";
            } else if (i < minPerBucket * 2) {
                label = "BUY";
            } else if (i >= size - minPerBucket) {
                label = "AVOID";
            } else {
                label = "HOLD";
            }

            dto.setRecommendation(label);
        }

        return adjusted;
    }

    private static Map<String, String> buildAssetNameMap() {
        Map<String, String> names = new HashMap<>();

        names.put("AAPL", "Apple Inc.");
        names.put("MSFT", "Microsoft Corporation");
        names.put("GOOGL", "Alphabet Inc.");
        names.put("AMZN", "Amazon.com Inc.");
        names.put("META", "Meta Platforms Inc.");
        names.put("NVDA", "NVIDIA Corporation");
        names.put("TSLA", "Tesla Inc.");
        names.put("JPM", "JPMorgan Chase & Co.");
        names.put("V", "Visa Inc.");
        names.put("NFLX", "Netflix Inc.");
        names.put("AMD", "Advanced Micro Devices Inc.");
        names.put("INTC", "Intel Corporation");
        names.put("ORCL", "Oracle Corporation");
        names.put("CRM", "Salesforce Inc.");
        names.put("COST", "Costco Wholesale Corporation");

        names.put("BTC-USD", "Bitcoin");
        names.put("ETH-USD", "Ethereum");
        names.put("SOL-USD", "Solana");
        names.put("BNB-USD", "BNB");
        names.put("XRP-USD", "XRP");
        names.put("ADA-USD", "Cardano");
        names.put("DOGE-USD", "Dogecoin");

        names.put("SPY", "SPDR S&P 500 ETF Trust");
        names.put("VOO", "Vanguard S&P 500 ETF");
        names.put("QQQ", "Invesco QQQ Trust");
        names.put("VTI", "Vanguard Total Stock Market ETF");
        names.put("IVV", "iShares Core S&P 500 ETF");
        names.put("DIA", "SPDR Dow Jones Industrial Average ETF Trust");
        names.put("IWM", "iShares Russell 2000 ETF");
        names.put("SCHD", "Schwab U.S. Dividend Equity ETF");

        names.put("BND", "Vanguard Total Bond Market ETF");
        names.put("AGG", "iShares Core U.S. Aggregate Bond ETF");
        names.put("TLT", "iShares 20+ Year Treasury Bond ETF");
        names.put("IEF", "iShares 7-10 Year Treasury Bond ETF");
        names.put("LQD", "iShares iBoxx $ Investment Grade Corporate Bond ETF");
        names.put("SHY", "iShares 1-3 Year Treasury Bond ETF");

        return names;
    }
}
