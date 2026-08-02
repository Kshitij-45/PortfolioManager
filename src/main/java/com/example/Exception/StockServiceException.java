package com.example.Exception;

public class StockServiceException extends RuntimeException {

    public StockServiceException(String symbol, String reason) {
        super("Failed to fetch stock data for symbol '" + symbol + "': " + reason);
    }
}
