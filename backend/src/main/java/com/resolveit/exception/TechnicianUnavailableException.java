package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 409: no active, qualified technician with capacity exists. */
public class TechnicianUnavailableException extends ItsmException {
    public TechnicianUnavailableException(String message) { super("TECHNICIAN_UNAVAILABLE", HttpStatus.CONFLICT, message); }
    public TechnicianUnavailableException() { this("No eligible technician is currently available."); }
}
