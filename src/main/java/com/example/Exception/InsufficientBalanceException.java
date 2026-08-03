package com.example.Exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(double availableBalance, double requiredAmount) {
        super("Insufficient balance. Available: "
                + String.format("%.2f", availableBalance)
                + ", Required: "
                + String.format("%.2f", requiredAmount));
    }
}
