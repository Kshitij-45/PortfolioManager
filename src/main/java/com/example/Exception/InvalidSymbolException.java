package com.example.exception;

public class InvalidSymbolException extends RuntimeException {

    public InvalidSymbolException(String assetType) {
        super("Invalid " + assetType + " symbol. Symbol must not be blank");
    }
}
