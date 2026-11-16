
package com.irs.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.irs.backend.model.Incident;
import com.irs.backend.model.User;
import com.irs.backend.model.enums.Impact;
import com.irs.backend.model.enums.Urgency;
import com.irs.backend.repository.IncidentRepository;
import com.irs.backend.service.sla.SlaService;
import com.irs.backend.strategy.PriorityCalculationStrategy;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final PriorityCalculationStrategy priorityStrategy;
    private final SlaService slaService;
    private final AuditService auditService;

    public IncidentService(
            IncidentRepository incidentRepository,
            PriorityCalculationStrategy priorityStrategy,
            SlaService slaService,
            AuditService auditService) {

        this.incidentRepository = incidentRepository;
        this.priorityStrategy = priorityStrategy;
        this.slaService = slaService;
        this.auditService = auditService;
    }

    @Transactional
    public Incident createIncident(
            String title,
            String description,
            String category,
            Impact impact,
            Urgency urgency,
            User reporter) {

        Incident incident = new Incident(title, description, category);

        incident.setImpact(impact);
        incident.setUrgency(urgency);
        incident.setReporter(reporter);

        incident.setPriority(
                priorityStrategy.calculate(impact, urgency));

        incident.setResponseDeadline(
                slaService.calculateResponseDeadline(incident));

        incident.setResolutionDeadline(
                slaService.calculateResolutionDeadline(incident));

        Incident savedIncident = incidentRepository.save(incident);

        auditService.log(
                savedIncident,
                reporter,
                "INCIDENT_CREATED",
                null,
                savedIncident.getStatus().name(),
                "Incident reported: " + savedIncident.getTitle());

        return savedIncident;
    }
}
