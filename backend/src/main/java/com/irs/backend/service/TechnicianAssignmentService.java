
package com.irs.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.irs.backend.model.Incident;
import com.irs.backend.model.Technician;
import com.irs.backend.model.User;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.repository.IncidentRepository;
import com.irs.backend.repository.TechnicianRepository;
import com.irs.backend.repository.UserRepository;
import com.irs.backend.strategy.assignment.TechnicianAssignmentStrategy;

@Service
public class TechnicianAssignmentService {

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final TechnicianAssignmentStrategy assignmentStrategy;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public TechnicianAssignmentService(
            IncidentRepository incidentRepository,
            TechnicianRepository technicianRepository,
            TechnicianAssignmentStrategy assignmentStrategy,
            AuditService auditService,
            UserRepository userRepository) {

        this.incidentRepository = incidentRepository;
        this.technicianRepository = technicianRepository;
        this.assignmentStrategy = assignmentStrategy;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Incident assignIncident(Long incidentId, User admin) {

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident not found"));

        if (incident.getAssignedTechnician() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Incident is already assigned");
        }

        if (incident.getStatus() != IncidentStatus.NEW) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only new incidents can be assigned");
        }

        List<Technician> technicians = technicianRepository.findAll();

        Technician technician = assignmentStrategy.assign(
                incident, technicians);

        incident.setAssignedTechnician(technician);
        incident.setStatus(IncidentStatus.ASSIGNED);

        technician.setActiveIncidentCount(
                technician.getActiveIncidentCount() + 1);

        technicianRepository.save(technician);

        Incident savedIncident = incidentRepository.save(incident);

        auditService.log(
                savedIncident,
                admin,
                "TECHNICIAN_ASSIGNED",
                IncidentStatus.NEW.name(),
                IncidentStatus.ASSIGNED.name(),
                "Incident assigned to technician: " + technician.getName()
        );

        return savedIncident;
    }
}
