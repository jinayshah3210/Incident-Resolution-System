package com.irs.backend.exception;

public class InvalidIncidentStateException extends RuntimeException {

    public InvalidIncidentStateException(String message) {
        super(message);
    }
}