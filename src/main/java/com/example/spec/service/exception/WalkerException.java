package com.example.spec.service.exception;

public class WalkerException extends RuntimeException {

    public WalkerException(String message) {
        super(message);
    }

    public WalkerException(String message, Throwable cause) {
        super(message, cause);
    }
}
