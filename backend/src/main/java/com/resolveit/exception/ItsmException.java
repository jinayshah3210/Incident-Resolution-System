package com.resolveit.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Root of the ResolveIT exception hierarchy. Every domain failure carries a stable error code,
 * an HTTP status and a message that is safe to show to the user.
 */
public abstract class ItsmException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    protected ItsmException(String errorCode, HttpStatus httpStatus, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected ItsmException(String errorCode, HttpStatus httpStatus, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getUserMessage() { return getMessage(); }
    /** Optional extra fields returned in the ApiError body (for example the existing incident id). */
    public Map<String, Object> getDetails() { return Map.of(); }
}
