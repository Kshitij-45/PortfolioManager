package com.example.exception;

public class InvalidPortfolioException extends RuntimeException {

    public InvalidPortfolioException(String message) {
        super(message);
    }
}
