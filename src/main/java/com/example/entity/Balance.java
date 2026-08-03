package com.example.entity;
import java.math.BigDecimal;
public class Balance {
    private Integer id;
    private BigDecimal availableBalance;
    public Balance() {
    }
    public Balance(Integer id, BigDecimal availableBalance) {
        this.id = id;
        this.availableBalance = availableBalance;
    }
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
}
