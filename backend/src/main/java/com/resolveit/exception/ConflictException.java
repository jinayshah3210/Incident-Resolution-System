package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 409: a uniqueness or business-rule conflict (duplicate name, feedback already given). */
public class ConflictException extends ItsmException {
    public ConflictException(String message) { super("CONFLICT", HttpStatus.CONFLICT, message); }
    public ConflictException() { this("This change conflicts with existing data."); }
}
