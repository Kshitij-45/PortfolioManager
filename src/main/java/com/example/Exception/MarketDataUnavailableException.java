package com.example.exception;

public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(String symbol, String reason) {
        super("Market data provider unavailable for symbol '" + symbol + "': " + reason);
    }
}
