package com.example.Exception;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(String assetType, String symbol) {
        super(assetType + " not found for symbol: " + symbol);
    }
}
