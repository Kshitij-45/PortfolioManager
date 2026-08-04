package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecommendationDTO {

    private List<AssetRecommendationDTO> stocks;
    private List<AssetRecommendationDTO> crypto;
    private List<AssetRecommendationDTO> funds;
    private List<AssetRecommendationDTO> bonds;
}
