
package com.irs.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "incident_audit_logs")
public class IncidentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime timestamp;

    private String action;

    private String previousStatus;

    private String newStatus;

    @Column(length = 2000)
    private String description;

    protected IncidentAuditLog() {
    }

    public IncidentAuditLog(
            Incident incident,
            User user,
            String action,
            String previousStatus,
            String newStatus,
            String description) {

        this.incident = incident;
        this.user = user;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getDescription() {
        return description;
    }
}
