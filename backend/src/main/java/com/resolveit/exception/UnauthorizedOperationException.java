package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 403: the authenticated user's role or ownership does not allow the action. */
public class UnauthorizedOperationException extends ItsmException {
    public UnauthorizedOperationException(String message) { super("UNAUTHORIZED_OPERATION", HttpStatus.FORBIDDEN, message); }
    public UnauthorizedOperationException() { this("You do not have access to this incident."); }
}
