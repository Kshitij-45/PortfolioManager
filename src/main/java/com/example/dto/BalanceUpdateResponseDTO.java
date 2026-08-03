package com.example.dto;

import java.math.BigDecimal;

public class BalanceUpdateResponseDTO {

    private String message;
    private BigDecimal addedAmount;
    private BigDecimal availableBalance;

    public BalanceUpdateResponseDTO() {
    }

    public BalanceUpdateResponseDTO(String message,
                                    BigDecimal addedAmount,
                                    BigDecimal availableBalance) {
        this.message = message;
        this.addedAmount = addedAmount;
        this.availableBalance = availableBalance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getAddedAmount() {
        return addedAmount;
    }

    public void setAddedAmount(BigDecimal addedAmount) {
        this.addedAmount = addedAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
}
