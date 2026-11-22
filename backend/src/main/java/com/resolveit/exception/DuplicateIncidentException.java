package com.resolveit.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** HTTP 409: the reporter already has a near-identical open incident. Resend with force=true to override. */
public class DuplicateIncidentException extends ItsmException {
    private final String existingIncidentId;

    public DuplicateIncidentException(String existingIncidentId) {
        super("DUPLICATE_INCIDENT", HttpStatus.CONFLICT, "Similar open ticket already exists: " + existingIncidentId);
        this.existingIncidentId = existingIncidentId;
    }

    public String getExistingIncidentId() { return existingIncidentId; }

    @Override public Map<String, Object> getDetails() { return Map.of("existingIncidentId", existingIncidentId); }
}
