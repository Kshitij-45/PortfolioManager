package com.example.controller;

import com.example.dto.StockQuoteDTO;
import com.example.dto.StockHistoryPointDTO;
import com.example.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * GET /api/stocks/{symbol}
     * Full quote: price, change, volume, 52-week range, P/E, etc.
     * Example: /api/stocks/AAPL
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<StockQuoteDTO> getQuote(@PathVariable String symbol) {
        StockQuoteDTO quote = stockService.getQuote(symbol);
        return ResponseEntity.ok(quote);
    }

    /**
     * GET /api/stocks/{symbol}/price
     * Returns only the current price for the given symbol.
     * Example: /api/stocks/TSLA/price
     */
    @GetMapping("/{symbol}/price")
    public ResponseEntity<BigDecimal> getCurrentPrice(@PathVariable String symbol) {
        BigDecimal price = stockService.getCurrentPrice(symbol);
        return ResponseEntity.ok(price);
    }

    /**
     * GET /api/stocks/{symbol}/history?range=1w|1m|1y
     * Returns historical close prices for charting.
     */
    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<StockHistoryPointDTO>> getHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String range) {
        List<StockHistoryPointDTO> history = stockService.getHistory(symbol, range);
        return ResponseEntity.ok(history);
    }

    /**
     * POST /api/stocks/batch
     * Accepts a list of symbols and returns quotes for all of them.
     * Body: ["AAPL", "TSLA", "MSFT"]
     */
    @PostMapping("/batch")
    public ResponseEntity<List<StockQuoteDTO>> getBatchQuotes(@RequestBody List<String> symbols) {
        List<StockQuoteDTO> quotes = stockService.getQuotes(symbols);
        return ResponseEntity.ok(quotes);
    }
}

