package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 404: the IT service chosen for an incident does not exist. */
public class ServiceNotFoundException extends ItsmException {
    public ServiceNotFoundException(String message) { super("SERVICE_NOT_FOUND", HttpStatus.NOT_FOUND, message); }
    public ServiceNotFoundException() { this("The selected IT service does not exist or is no longer active."); }
}
