package com.example.controller;

import com.example.entity.Portfolio;
import com.example.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    // GET all portfolios
    @GetMapping
    public ResponseEntity<List<Portfolio>> getAllPortfolios() {
        List<Portfolio> portfolios = portfolioService.getAllPortfolios();
        return ResponseEntity.ok(portfolios);
    }

    // GET portfolio by id
    @GetMapping("/{id}")
    public ResponseEntity<?> getPortfolioById(@PathVariable Integer id) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioById(id);
            return ResponseEntity.ok(portfolio);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // POST create portfolio
    @PostMapping
    public ResponseEntity<?> createPortfolio(@RequestBody Portfolio portfolio) {
        try {
            Portfolio created = portfolioService.createPortfolio(portfolio);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // PUT update portfolio
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePortfolio(@PathVariable Integer id, @RequestBody Portfolio portfolio) {
        try {
            Portfolio updated = portfolioService.updatePortfolio(id, portfolio);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // DELETE portfolio
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePortfolio(@PathVariable Integer id) {
        try {
            portfolioService.deletePortfolio(id);
            return ResponseEntity.ok("Portfolio deleted successfully with id: " + id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // GET check if portfolio exists
    @GetMapping("/{id}/exists")
    public ResponseEntity<?> existsById(@PathVariable Integer id) {
        try {
            boolean exists = portfolioService.existsById(id);
            return ResponseEntity.ok(exists);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}