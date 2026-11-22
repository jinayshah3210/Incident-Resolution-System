package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** Raised when no SLA policy exists; SlaService catches it and falls back to defaults. */
public class MissingSlaConfigurationException extends ItsmException {
    public MissingSlaConfigurationException(String message) { super("MISSING_SLA_CONFIGURATION", HttpStatus.INTERNAL_SERVER_ERROR, message); }
    public MissingSlaConfigurationException() { this("No SLA policy is configured for this priority."); }
}
