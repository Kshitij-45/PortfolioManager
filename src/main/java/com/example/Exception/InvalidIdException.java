package com.example.Exception;

public class InvalidIdException extends RuntimeException {

    public InvalidIdException(Integer id) {
        super("Invalid id: " + id);
    }

    public InvalidIdException(String message) {
        super(message);
    }
}
