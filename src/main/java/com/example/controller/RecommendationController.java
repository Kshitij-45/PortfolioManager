package com.example.controller;

import com.example.dto.AssetRecommendationDTO;
import com.example.dto.RecommendationDTO;
import com.example.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<RecommendationDTO> getAllRecommendations() {
        return ResponseEntity.ok(recommendationService.getAllRecommendations());
    }

    @GetMapping("/stocks")
    public ResponseEntity<List<AssetRecommendationDTO>> getStockRecommendations() {
        return ResponseEntity.ok(recommendationService.getStockRecommendations());
    }

    @GetMapping("/crypto")
    public ResponseEntity<List<AssetRecommendationDTO>> getCryptoRecommendations() {
        return ResponseEntity.ok(recommendationService.getCryptoRecommendations());
    }

    @GetMapping("/funds")
    public ResponseEntity<List<AssetRecommendationDTO>> getFundRecommendations() {
        return ResponseEntity.ok(recommendationService.getFundRecommendations());
    }

    @GetMapping("/bonds")
    public ResponseEntity<List<AssetRecommendationDTO>> getBondRecommendations() {
        return ResponseEntity.ok(recommendationService.getBondRecommendations());
    }
}
