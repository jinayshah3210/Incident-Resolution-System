package com.irs.backend.service.sla;

import java.time.LocalDateTime;
import com.irs.backend.model.Incident;

public class SlaService {

    private final SlaPolicy slaPolicy;

    public SlaService(SlaPolicy slaPolicy) {
        this.slaPolicy = slaPolicy;
    }

    public void setDeadlines(Incident incident) {

        LocalDateTime responseDeadline = calculateResponseDeadline(incident);

        LocalDateTime resolutionDeadline = calculateResolutionDeadline(incident);

        incident.setResponseDeadline(responseDeadline);
        incident.setResolutionDeadline(resolutionDeadline);
    }

    public LocalDateTime calculateResponseDeadline(Incident incident) {
        return incident.getCreatedAt()
                .plus(slaPolicy.getResponseTime(incident.getPriority()));
    }

    public LocalDateTime calculateResolutionDeadline(Incident incident) {
        return incident.getCreatedAt()
                .plus(slaPolicy.getResolutionTime(incident.getPriority()));
    }

    public boolean isResponseSlaBreached(Incident incident) {

        if (incident.getRespondedAt() != null) {
            return incident.getRespondedAt()
                    .isAfter(incident.getResponseDeadline());
        }

        return LocalDateTime.now()
                .isAfter(incident.getResponseDeadline());
    }

    public boolean isResolutionSlaBreached(Incident incident) {

        // If incident has been resolved, check its actual resolution time
        if (incident.getResolvedAt() != null) {
            return incident.getResolvedAt()
                    .isAfter(incident.getResolutionDeadline());
        }

        // If not resolved yet, check whether the deadline has passed
        return LocalDateTime.now()
                .isAfter(incident.getResolutionDeadline());
    }
}