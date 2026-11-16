
package com.irs.backend.dto;

import com.irs.backend.model.Incident;
import com.irs.backend.model.Technician;
import com.irs.backend.model.enums.*;

import java.time.LocalDateTime;

public class IncidentResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String service;
    private Impact impact;
    private Urgency urgency;
    private Priority priority;
    private IncidentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime responseDeadline;
    private LocalDateTime resolutionDeadline;
    private Long reporterId;
    private String reporter;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private String technician;

    public IncidentResponse(Incident incident) {
        this.id = incident.getId();
        this.title = incident.getTitle();
        this.description = incident.getDescription();
        this.category = incident.getCategory();

        // Compatibility with existing frontend fields
        this.service = incident.getCategory();

        this.impact = incident.getImpact();
        this.urgency = incident.getUrgency();
        this.priority = incident.getPriority();
        this.status = incident.getStatus();
        this.createdAt = incident.getCreatedAt();
        this.responseDeadline = incident.getResponseDeadline();
        this.resolutionDeadline = incident.getResolutionDeadline();

        if (incident.getReporter() != null) {
            this.reporterId = incident.getReporter().getId();
            this.reporter = incident.getReporter().getName();
        }

        Technician assigned = incident.getAssignedTechnician();

        if (assigned != null) {
            this.assignedTechnicianId = assigned.getId();
            this.assignedTechnicianName = assigned.getName();
            this.technician = assigned.getName();
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getService() { return service; }
    public Impact getImpact() { return impact; }
    public Urgency getUrgency() { return urgency; }
    public Priority getPriority() { return priority; }
    public IncidentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public LocalDateTime getResolutionDeadline() { return resolutionDeadline; }
    public Long getReporterId() { return reporterId; }
    public String getReporter() { return reporter; }
    public Long getAssignedTechnicianId() { return assignedTechnicianId; }
    public String getAssignedTechnicianName() { return assignedTechnicianName; }
    public String getTechnician() { return technician; }
}
