package com.example.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(double availableBalance, double requiredAmount) {
        super("Insufficient balance. Available: "
                + String.format("%.2f", availableBalance)
                + ", Required: "
                + String.format("%.2f", requiredAmount));
    }

    public InsufficientBalanceException(String string) {
        
        super(string);
    }
}
