package com.akiba.backend.config.exception;

public class SelfFollowException extends RuntimeException {
    public SelfFollowException(String message) {
        super(message);
    }
}