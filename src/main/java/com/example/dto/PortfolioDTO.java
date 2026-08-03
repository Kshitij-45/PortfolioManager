package com.example.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PortfolioDTO {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Asset type is required")
    private String assetType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Buy price is required")
    @DecimalMin(value = "0.01", message = "Buy price must be greater than 0")
    private Double buyPrice;

    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.01", message = "Current price must be greater than 0")
    private Double currentPrice;

    @NotNull(message = "Purchase date is required")
    @PastOrPresent(message = "Purchase date cannot be in the future")
    private LocalDate purchaseDate;
}