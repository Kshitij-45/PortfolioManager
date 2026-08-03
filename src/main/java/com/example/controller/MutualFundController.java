package com.example.controller;

import com.example.dto.MutualFundQuoteDTO;
import com.example.service.MutualFundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/funds")
@CrossOrigin(origins = "*")
public class MutualFundController {

    private final MutualFundService mutualFundService;

    public MutualFundController(MutualFundService mutualFundService) {
        this.mutualFundService = mutualFundService;
    }

    /**
     * GET /api/funds/{symbol}
     * Full quote (NAV) for a mutual fund.
     * Examples: /api/funds/VFINX  /api/funds/FXAIX  /api/funds/VTSAX
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<MutualFundQuoteDTO> getQuote(@PathVariable String symbol) {
        return ResponseEntity.ok(mutualFundService.getQuote(symbol));
    }

    /**
     * GET /api/funds/{symbol}/nav
     * Current NAV only.
     */
    @GetMapping("/{symbol}/nav")
    public ResponseEntity<BigDecimal> getCurrentNav(@PathVariable String symbol) {
        return ResponseEntity.ok(mutualFundService.getCurrentNav(symbol));
    }

    /**
     * POST /api/funds/batch
     * Quotes for multiple fund symbols.
     * Body: ["VFINX", "FXAIX", "VTSAX"]
     */
    @PostMapping("/batch")
    public ResponseEntity<List<MutualFundQuoteDTO>> getBatchQuotes(@RequestBody List<String> symbols) {
        return ResponseEntity.ok(mutualFundService.getQuotes(symbols));
    }
}
