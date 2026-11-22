package com.resolveit.domain.sla;

import com.resolveit.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/** A recorded breach of one SLA clock. Dismissing hides the alert but keeps the record. */
@Entity
@Table(name = "sla_violations")
public class SlaViolation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;
    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 12)
    private ViolationType type;
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;
    @Column(name = "delay_minutes", nullable = false)
    private long delayMinutes;
    @Column(name = "technician_id", length = 20)
    private String technicianId;
    @Column(nullable = false)
    private boolean dismissed;
    @Column(name = "dismissed_by", length = 20)
    private String dismissedBy;

    protected SlaViolation() { }

    public SlaViolation(Incident incident, ViolationType type, Instant dueAt, Instant detectedAt) {
        this.incident = incident;
        this.type = type;
        this.dueAt = dueAt;
        this.detectedAt = detectedAt;
        this.delayMinutes = Math.max(0, Duration.between(dueAt, detectedAt).toMinutes());
        this.technicianId = incident.getTechnicianId();
    }

    public void dismiss(String adminId) {
        this.dismissed = true;
        this.dismissedBy = adminId;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public ViolationType getType() { return type; }
    public Instant getDueAt() { return dueAt; }
    public Instant getDetectedAt() { return detectedAt; }
    public long getDelayMinutes() { return delayMinutes; }
    public String getTechnicianId() { return technicianId; }
    public boolean isDismissed() { return dismissed; }
    public String getDismissedBy() { return dismissedBy; }
}
