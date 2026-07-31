package com.example.service;

import com.example.Exception.StockNotFoundException;
import com.example.dto.StockQuoteDTO;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;
import yahoofinance.quotes.stock.StockStats;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    /**
     * Get a full quote for a single stock symbol (e.g. "AAPL", "TSLA").
     */
    public StockQuoteDTO getQuote(String symbol) {
        String upperSymbol = symbol.trim().toUpperCase();
        try {
            Stock stock = YahooFinance.get(upperSymbol);
            if (stock == null || stock.getQuote() == null) {
                throw new StockNotFoundException(upperSymbol);
            }
            return mapToDTO(stock);
        } catch (IOException e) {
            throw new StockNotFoundException(upperSymbol);
        }
    }

    /**
     * Get the current price only for a symbol.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        String upperSymbol = symbol.trim().toUpperCase();
        try {
            Stock stock = YahooFinance.get(upperSymbol);
            if (stock == null || stock.getQuote() == null) {
                throw new StockNotFoundException(upperSymbol);
            }
            BigDecimal price = stock.getQuote().getPrice();
            if (price == null) {
                throw new StockNotFoundException(upperSymbol);
            }
            return price;
        } catch (IOException e) {
            throw new StockNotFoundException(upperSymbol);
        }
    }

    /**
     * Get quotes for multiple symbols at once.
     */
    public List<StockQuoteDTO> getQuotes(List<String> symbols) {
        String[] symbolArray = symbols.stream()
                .map(s -> s.trim().toUpperCase())
                .toArray(String[]::new);
        try {
            Map<String, Stock> stocks = YahooFinance.get(symbolArray);
            List<StockQuoteDTO> results = new ArrayList<>();
            for (String sym : symbolArray) {
                Stock stock = stocks.get(sym);
                if (stock != null && stock.getQuote() != null) {
                    results.add(mapToDTO(stock));
                }
            }
            return results;
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch stock data: " + e.getMessage(), e);
        }
    }

    private StockQuoteDTO mapToDTO(Stock stock) {
        StockQuote quote = stock.getQuote();
        StockStats stats = stock.getStats();

        return StockQuoteDTO.builder()
                .symbol(stock.getSymbol())
                .companyName(stock.getName())
                .exchange(stock.getStockExchange())
                .currency(stock.getCurrency())
                .currentPrice(quote.getPrice())
                .previousClose(quote.getPreviousClose())
                .open(quote.getOpen())
                .dayHigh(quote.getDayHigh())
                .dayLow(quote.getDayLow())
                .priceChange(quote.getChange())
                .priceChangePercent(quote.getChangeInPercent())
                .volume(quote.getVolume())
                .avgVolume(quote.getAvgVolume())
                .fiftyTwoWeekHigh(stats != null ? stats.getYearHigh() : null)
                .fiftyTwoWeekLow(stats != null ? stats.getYearLow() : null)
                .marketCap(stats != null ? stats.getMarketCap() : null)
                .peRatio(stats != null ? stats.getPe() : null)
                .eps(stats != null ? stats.getEps() : null)
                .dividendYield(quote.getAnnualYieldPercent())
                .build();
    }
}

