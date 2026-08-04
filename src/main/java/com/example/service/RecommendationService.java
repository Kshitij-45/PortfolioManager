package com.example.service;

import com.example.dto.AssetRecommendationDTO;
import com.example.dto.RecommendationDTO;

import java.util.List;

public interface RecommendationService {

    RecommendationDTO getAllRecommendations();

    List<AssetRecommendationDTO> getStockRecommendations();

    List<AssetRecommendationDTO> getCryptoRecommendations();

    List<AssetRecommendationDTO> getFundRecommendations();

    List<AssetRecommendationDTO> getBondRecommendations();
}
