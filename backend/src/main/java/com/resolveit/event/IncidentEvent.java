package com.resolveit.event;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Observer pattern: an immutable event published after each step of an incident's life.
 * Attributes carry optional extras such as "recipient" or "recipients" (comma separated user ids).
 */
public record IncidentEvent(IncidentEventType type, Incident incident, String actorId, String detail,
                            IncidentStatus previousStatus, Instant occurredAt, Map<String, String> attributes) {
    public static final String SYSTEM = "SYSTEM";

    public IncidentEvent {
        attributes = Map.copyOf(attributes);
    }

    public static IncidentEvent of(IncidentEventType type, Incident incident, String actorId, String detail,
                                   IncidentStatus previousStatus, Instant at) {
        return new IncidentEvent(type, incident, actorId == null ? SYSTEM : actorId, detail, previousStatus, at, Map.of());
    }

    public IncidentEvent with(String key, String value) {
        if (value == null) return this;
        Map<String, String> next = new LinkedHashMap<>(attributes);
        next.put(key, value);
        return new IncidentEvent(type, incident, actorId, detail, previousStatus, occurredAt, next);
    }

    public String attribute(String key) { return attributes.get(key); }

    /** True when the event moved the incident to a different status. */
    public boolean changedStatus() { return previousStatus != null && previousStatus != incident.getStatus(); }
}
