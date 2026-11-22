package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 404: an incident, user or other record does not exist. */
public class ResourceNotFoundException extends ItsmException {
    public ResourceNotFoundException(String message) { super("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, message); }
    public ResourceNotFoundException() { this("The requested record was not found."); }
}
