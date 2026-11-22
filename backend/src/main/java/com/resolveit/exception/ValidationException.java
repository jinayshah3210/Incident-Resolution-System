package com.resolveit.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** HTTP 400: invalid user input, optionally with per-field messages. */
public class ValidationException extends ItsmException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) { this(message, Map.of()); }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super("INVALID_INPUT", HttpStatus.BAD_REQUEST, message);
        this.fieldErrors = new LinkedHashMap<>(fieldErrors);
    }

    public static ValidationException field(String field, String message) {
        return new ValidationException(message, Map.of(field, message));
    }

    public Map<String, String> getFieldErrors() { return Map.copyOf(fieldErrors); }
}
