package com.example.controller;

import com.example.dto.CryptoQuoteDTO;
import com.example.service.CryptoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "*")
public class CryptoController {

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    /**
     * GET /api/crypto/{symbol}
     * Full quote for a crypto symbol.
     * Example: /api/crypto/BTC-USD  or  /api/crypto/ETH-USD
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<CryptoQuoteDTO> getQuote(@PathVariable String symbol) {
        return ResponseEntity.ok(cryptoService.getQuote(symbol));
    }

    /**
     * GET /api/crypto/{symbol}/price
     * Current price only.
     */
    @GetMapping("/{symbol}/price")
    public ResponseEntity<BigDecimal> getCurrentPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(cryptoService.getCurrentPrice(symbol));
    }

    /**
     * POST /api/crypto/batch
     * Quotes for multiple symbols.
     * Body: ["BTC-USD", "ETH-USD", "SOL-USD"]
     */
    @PostMapping("/batch")
    public ResponseEntity<List<CryptoQuoteDTO>> getBatchQuotes(@RequestBody List<String> symbols) {
        return ResponseEntity.ok(cryptoService.getQuotes(symbols));
    }
}
