package com.example.exception;

public class RecommendationProcessingException extends RuntimeException {

    public RecommendationProcessingException(String message) {
        super(message);
    }

    public RecommendationProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
