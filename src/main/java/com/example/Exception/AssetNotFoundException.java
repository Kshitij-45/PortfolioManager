package com.example.exception;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(String assetType, String symbol) {
        super(assetType + " not found for symbol: " + symbol);
    }
}
