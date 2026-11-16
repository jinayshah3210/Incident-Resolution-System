
package com.irs.backend.controller;

import java.util.List;

import com.irs.backend.dto.CreateIncidentRequest;
import com.irs.backend.dto.IncidentResponse;
import com.irs.backend.dto.UpdateIncidentStatusRequest;
import com.irs.backend.model.Admin;
import com.irs.backend.model.Employee;
import com.irs.backend.model.Incident;
import com.irs.backend.model.IncidentAuditLog;
import com.irs.backend.model.Technician;
import com.irs.backend.model.User;
import com.irs.backend.repository.IncidentAuditRepository;
import com.irs.backend.repository.IncidentRepository;
import com.irs.backend.repository.UserRepository;
import com.irs.backend.service.IncidentLifecycleService;
import com.irs.backend.service.IncidentService;
import com.irs.backend.service.TechnicianAssignmentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final TechnicianAssignmentService assignmentService;
    private final IncidentLifecycleService lifecycleService;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentAuditRepository auditRepository;

    public IncidentController(
            IncidentService incidentService,
            TechnicianAssignmentService assignmentService,
            IncidentLifecycleService lifecycleService,
            UserRepository userRepository,
            IncidentRepository incidentRepository,
            IncidentAuditRepository auditRepository) {

        this.incidentService = incidentService;
        this.assignmentService = assignmentService;
        this.lifecycleService = lifecycleService;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.auditRepository = auditRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("User not found"));
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (!(user instanceof Employee)) {
            throw new AccessDeniedException(
                    "Only employees can report incidents");
        }

        Incident incident = incidentService.createIncident(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getImpact(),
                request.getUrgency(),
                user
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new IncidentResponse(incident));
    }

    @PostMapping("/{incidentId}/assign")
    public ResponseEntity<IncidentResponse> assignIncident(
            @PathVariable Long incidentId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (!(user instanceof Admin)) {
            throw new AccessDeniedException(
                    "Only admins can assign incidents");
        }

        Incident incident = assignmentService.assignIncident(
                incidentId, user);

        return ResponseEntity.ok(new IncidentResponse(incident));
    }

    @PatchMapping("/{incidentId}/status")
    public ResponseEntity<IncidentResponse> updateIncidentStatus(
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Incident incident = lifecycleService.transition(
                incidentId,
                request.getStatus(),
                user
        );

        return ResponseEntity.ok(new IncidentResponse(incident));
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getIncidents(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        List<Incident> incidents;

        if (user instanceof Admin) {
            incidents = incidentRepository.findAll();
        } else if (user instanceof Employee) {
            incidents = incidentRepository.findByReporterId(user.getId());
        } else if (user instanceof Technician) {
            incidents = incidentRepository.findByAssignedTechnicianId(user.getId());
        } else {
            throw new AccessDeniedException("Access denied");
        }

        List<IncidentResponse> response = incidents.stream()
                .map(IncidentResponse::new)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> getIncident(
            @PathVariable Long incidentId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident not found"));

        if (!canAccessIncident(user, incident)) {
            throw new AccessDeniedException(
                    "You are not allowed to view this incident");
        }

        return ResponseEntity.ok(new IncidentResponse(incident));
    }

    @GetMapping("/{incidentId}/history")
    public ResponseEntity<List<IncidentAuditLog>> getIncidentHistory(
            @PathVariable Long incidentId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident not found"));

        if (!canAccessIncident(user, incident)) {
            throw new AccessDeniedException("Access denied");
        }

        return ResponseEntity.ok(
                auditRepository.findByIncidentIdOrderByTimestampAsc(incidentId));
    }

    private boolean canAccessIncident(User user, Incident incident) {

        return user instanceof Admin
                || (user instanceof Employee
                    && incident.getReporter() != null
                    && incident.getReporter().getId().equals(user.getId()))
                || (user instanceof Technician
                    && incident.getAssignedTechnician() != null
                    && incident.getAssignedTechnician().getId().equals(user.getId()));
    }
}
