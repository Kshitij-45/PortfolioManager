package com.example.controller;

import com.example.dto.PortfolioDTO;
import com.example.entity.Portfolio;
import com.example.service.PortfolioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins="*")
@Validated
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
    public ResponseEntity<Portfolio> getPortfolioById(@PathVariable Integer id) {
        Portfolio portfolio = portfolioService.getPortfolioById(id);
        return ResponseEntity.ok(portfolio);
    }

    // POST create portfolio
    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@Valid @RequestBody PortfolioDTO portfolioDTO) {
        Portfolio created = portfolioService.createPortfolio(portfolioDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT update portfolio
    @PutMapping("/{id}")
    public ResponseEntity<Portfolio> updatePortfolio(@PathVariable Integer id, @Valid @RequestBody PortfolioDTO portfolioDTO) {
        Portfolio updated = portfolioService.updatePortfolio(id, portfolioDTO);
        return ResponseEntity.ok(updated);
    }

    // DELETE portfolio
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePortfolio(@PathVariable Integer id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.ok("Portfolio deleted successfully with id: " + id);
    }

    // GET check if portfolio exists
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> existsById(@PathVariable Integer id) {
        boolean exists = portfolioService.existsById(id);
        return ResponseEntity.ok(exists);
    }
}