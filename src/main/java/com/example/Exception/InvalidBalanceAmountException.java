package com.example.exception;

public class InvalidBalanceAmountException extends RuntimeException {

    public InvalidBalanceAmountException(String message) {
        super(message);
    }
}
