package com.resolveit.domain.sla;

import com.resolveit.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One step taken by the escalation chain for an incident (one row per level). */
@Entity
@Table(name = "escalations")
public class Escalation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;
    @Column(nullable = false)
    private int level;
    @Column(name = "target_role", nullable = false, length = 30)
    private String targetRole;
    @Column(name = "target_user", length = 20)
    private String targetUser;
    @Column(nullable = false, length = 300)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Escalation() { }

    public Escalation(Incident incident, int level, String targetRole, String targetUser, String reason, Instant createdAt) {
        this.incident = incident;
        this.level = level;
        this.targetRole = targetRole;
        this.targetUser = targetUser;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public int getLevel() { return level; }
    public String getTargetRole() { return targetRole; }
    public String getTargetUser() { return targetUser; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
