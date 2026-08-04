package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProfitSummaryDTO {

    private LocalDate date;
    private BigDecimal totalProfit;

    public ProfitSummaryDTO(LocalDate date, BigDecimal totalProfit) {
        this.date = date;
        this.totalProfit = totalProfit;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getTotalProfit() { return totalProfit; }
    public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
}
