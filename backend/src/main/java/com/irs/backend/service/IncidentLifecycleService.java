
package com.irs.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.irs.backend.model.Incident;
import com.irs.backend.model.User;
import com.irs.backend.model.Employee;
import com.irs.backend.model.Technician;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.repository.IncidentRepository;

import java.time.LocalDateTime;

@Service
public class IncidentLifecycleService {

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    public IncidentLifecycleService(
            IncidentRepository incidentRepository,
            AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Incident transition(
            Long incidentId,
            IncidentStatus targetStatus,
            User actor) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident not found"));

        IncidentStatus current = incident.getStatus();

        Technician technician = incident.getAssignedTechnician();

        boolean isAssignedTechnician =
                actor instanceof Technician
                && technician != null
                && technician.getId().equals(actor.getId());

        boolean isReporter =
                actor instanceof Employee
                && incident.getReporter().getId().equals(actor.getId());

        boolean valid = false;

        if (isAssignedTechnician) {
            valid =
                    (current == IncidentStatus.ASSIGNED
                            && targetStatus == IncidentStatus.ACKNOWLEDGED)
                    || (current == IncidentStatus.ACKNOWLEDGED
                            && targetStatus == IncidentStatus.IN_PROGRESS)
                    || (current == IncidentStatus.IN_PROGRESS
                            && targetStatus == IncidentStatus.ON_HOLD)
                    || (current == IncidentStatus.ON_HOLD
                            && targetStatus == IncidentStatus.IN_PROGRESS)
                    || (current == IncidentStatus.IN_PROGRESS
                            && targetStatus == IncidentStatus.RESOLVED)
                    || (current == IncidentStatus.REOPENED
                            && targetStatus == IncidentStatus.IN_PROGRESS);
        }

        if (isReporter) {
            valid =
                    (current == IncidentStatus.RESOLVED
                            && targetStatus == IncidentStatus.VERIFIED)
                    || (current == IncidentStatus.VERIFIED
                            && targetStatus == IncidentStatus.CLOSED)
                    || ((current == IncidentStatus.RESOLVED
                            || current == IncidentStatus.VERIFIED)
                            && targetStatus == IncidentStatus.REOPENED);
        }

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid status transition or insufficient permissions");
        }

        if (targetStatus == IncidentStatus.ACKNOWLEDGED
                && incident.getRespondedAt() == null) {
            incident.setRespondedAt(LocalDateTime.now());
        }

        if (targetStatus == IncidentStatus.RESOLVED && technician != null) {
            technician.setActiveIncidentCount(
                    Math.max(0, technician.getActiveIncidentCount() - 1));
        }

        if (targetStatus == IncidentStatus.REOPENED && technician != null) {
            technician.setActiveIncidentCount(
                    technician.getActiveIncidentCount() + 1);
        }

        incident.setStatus(targetStatus);

        Incident savedIncident = incidentRepository.save(incident);

        auditService.log(
                savedIncident,
                actor,
                "STATUS_CHANGED",
                current.name(),
                targetStatus.name(),
                "Incident status changed from " + current
                        + " to " + targetStatus
        );

        return savedIncident;
    }
}
