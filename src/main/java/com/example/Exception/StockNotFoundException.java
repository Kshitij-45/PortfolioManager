package com.example.Exception;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(String symbol) {
        super("Stock not found for symbol: " + symbol);
    }
}
