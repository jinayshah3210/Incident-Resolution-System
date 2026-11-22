package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 401: wrong login, wrong password or wrong portal; the message never reveals which. */
public class InvalidCredentialsException extends ItsmException {
    public InvalidCredentialsException(String message) { super("INVALID_LOGIN", HttpStatus.UNAUTHORIZED, message); }
    public InvalidCredentialsException() { this("Email or password is incorrect."); }
}
