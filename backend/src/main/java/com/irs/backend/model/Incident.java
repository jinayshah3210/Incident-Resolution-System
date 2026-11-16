
package com.irs.backend.model;

import com.irs.backend.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String description;

    private String category;

    @Enumerated(EnumType.STRING)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime responseDeadline;
    private LocalDateTime resolutionDeadline;
    private LocalDateTime respondedAt;
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private Technician assignedTechnician;

    protected Incident() {
    }

    public Incident(String title, String description, String category) {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = IncidentStatus.NEW;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public IncidentStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public Impact getImpact() { return impact; }
    public Urgency getUrgency() { return urgency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getResponseDeadline() { return responseDeadline; }
    public LocalDateTime getResolutionDeadline() { return resolutionDeadline; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public User getReporter() { return reporter; }
    public Technician getAssignedTechnician() { return assignedTechnician; }

    public void setImpact(Impact impact) { this.impact = impact; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public void setAssignedTechnician(Technician technician) {
        this.assignedTechnician = technician;
    }

    public void setResponseDeadline(LocalDateTime deadline) {
        this.responseDeadline = deadline;
    }

    public void setResolutionDeadline(LocalDateTime deadline) {
        this.resolutionDeadline = deadline;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
