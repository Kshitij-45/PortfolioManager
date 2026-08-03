package com.example.exception;

public class MarketDataParsingException extends RuntimeException {

    public MarketDataParsingException(String symbol, String reason) {
        super("Invalid market data response for symbol '" + symbol + "': " + reason);
    }
}
