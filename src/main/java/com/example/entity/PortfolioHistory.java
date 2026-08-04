package com.example.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioHistory {

    private Integer id;
    private Integer portfolioId;
    private String symbol;
    private LocalDate recordedDate;
    private Double buyPrice;
    private Double currentPrice;
    private Integer quantity;
    private BigDecimal profit;  // (currentPrice - buyPrice) * quantity

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Integer portfolioId) { this.portfolioId = portfolioId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public LocalDate getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDate recordedDate) { this.recordedDate = recordedDate; }

    public Double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(Double buyPrice) { this.buyPrice = buyPrice; }

    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }
}
