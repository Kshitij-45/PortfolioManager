package com.example.exception;

public class EmptyBatchRequestException extends RuntimeException {

    public EmptyBatchRequestException(String assetType) {
        super("At least one " + assetType + " symbol is required for batch request");
    }
}
