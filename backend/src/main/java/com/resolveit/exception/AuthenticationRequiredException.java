package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 401: missing, invalid, revoked or expired token. */
public class AuthenticationRequiredException extends ItsmException {
    public AuthenticationRequiredException(String message) { super("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, message); }
    public AuthenticationRequiredException() { this("Your session has expired. Please sign in again."); }
}
