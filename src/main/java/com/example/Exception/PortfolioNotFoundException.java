package com.example.exception;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(Integer id) {
        super("Portfolio not found with id: " + id);
    }

    public PortfolioNotFoundException(String message) {
        super(message);
    }
}
