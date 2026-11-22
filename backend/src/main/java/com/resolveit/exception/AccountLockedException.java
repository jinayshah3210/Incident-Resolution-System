package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 423: five failed logins lock the account for fifteen minutes. */
public class AccountLockedException extends ItsmException {
    public AccountLockedException(String message) { super("ACCOUNT_LOCKED", HttpStatus.LOCKED, message); }
    public AccountLockedException() { this("This account is temporarily locked. Try again in 15 minutes."); }
}
