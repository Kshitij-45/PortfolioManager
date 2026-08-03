package com.example.controller;

import com.example.dto.BondQuoteDTO;
import com.example.service.BondService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bonds")
@CrossOrigin(origins = "*")
public class BondController {

    private final BondService bondService;

    public BondController(BondService bondService) {
        this.bondService = bondService;
    }

    /**
     * GET /api/bonds/{symbol}
     * Full quote for a bond or bond ETF.
     * Examples: /api/bonds/TLT  /api/bonds/AGG  /api/bonds/%5ETNX  (^TNX URL-encoded)
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<BondQuoteDTO> getQuote(@PathVariable String symbol) {
        return ResponseEntity.ok(bondService.getQuote(symbol));
    }

    /**
     * GET /api/bonds/{symbol}/price
     * Current price only.
     */
    @GetMapping("/{symbol}/price")
    public ResponseEntity<BigDecimal> getCurrentPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(bondService.getCurrentPrice(symbol));
    }

    /**
     * POST /api/bonds/batch
     * Quotes for multiple bond symbols.
     * Body: ["TLT", "AGG", "BND"]
     */
    @PostMapping("/batch")
    public ResponseEntity<List<BondQuoteDTO>> getBatchQuotes(@RequestBody List<String> symbols) {
        return ResponseEntity.ok(bondService.getQuotes(symbols));
    }
}
