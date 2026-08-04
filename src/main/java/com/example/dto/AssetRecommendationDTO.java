package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AssetRecommendationDTO {

    private String ticker;
    private String companyName;
    private BigDecimal currentPrice;
    private String assetType;
    private String recommendation;
    private int score;
    private int confidence;
    private String riskLevel;
    private List<String> reasons;
}
