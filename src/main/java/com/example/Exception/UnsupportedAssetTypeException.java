package com.example.exception;

public class UnsupportedAssetTypeException extends RuntimeException {

    public UnsupportedAssetTypeException(String assetType) {
        super("Unsupported asset type: " + assetType + ". Supported types are Stock, Bond, Crypto, Mutual Fund");
    }
}
