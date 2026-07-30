package com.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PortfolioDTO {

    @NotBlank(message = "Asset name is required")
    private String assetName;

    @NotBlank(message = "Asset type is required")
    private String assetType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Buy price is required")
    @Min(value = 0, message = "Buy price cannot be negative")
    private Double buyPrice;

    @NotNull(message = "Current price is required")
    @Min(value = 0, message = "Current price cannot be negative")
    private Double currentPrice;
}