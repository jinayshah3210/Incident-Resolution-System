package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 502: a network call to an external dependency (mail gateway, optional AI classifier) failed. */
public class ExternalServiceException extends ItsmException {
    public ExternalServiceException(String message, Throwable cause) {
        super("NETWORK_FAILURE", HttpStatus.BAD_GATEWAY, message, cause);
    }
    public ExternalServiceException(String message) { super("NETWORK_FAILURE", HttpStatus.BAD_GATEWAY, message); }
}
