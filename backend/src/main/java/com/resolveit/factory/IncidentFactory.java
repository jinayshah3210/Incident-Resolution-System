package com.resolveit.factory;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.repository.IncidentRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Factory pattern: builds a NEW incident with the next public id (INC1001, INC1002, ...). */
@Component
public class IncidentFactory {
    private final IncidentRepository incidents;

    public IncidentFactory(IncidentRepository incidents) { this.incidents = incidents; }

    public synchronized Incident create(PortalUser reporter, String title, String description, int affectedUsers, Instant at) {
        long next = Math.max(1001, incidents.findMaxSequenceNumber() + 1);
        return new Incident(next, title, description, reporter, affectedUsers, at);
    }
}
