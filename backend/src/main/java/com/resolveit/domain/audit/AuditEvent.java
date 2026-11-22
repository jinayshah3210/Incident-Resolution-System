package com.resolveit.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Append-only audit record. It has no setters, Hibernate treats it as immutable, and
 * AuditEventRepository exposes only save and read methods (no update or delete path).
 */
@Entity
@Immutable
@Table(name = "audit_events")
public class AuditEvent {
    public static final String SYSTEM_SCOPE = "SYSTEM";

    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(nullable = false, length = 40)
    private String actor;
    @Column(name = "incident_id", nullable = false, length = 20)
    private String incidentId;
    @Column(nullable = false, length = 60)
    private String action;
    @Column(length = 4000)
    private String detail;
    @Column(name = "previous_status", length = 20)
    private String previousStatus;
    @Column(name = "new_status", length = 20)
    private String newStatus;

    protected AuditEvent() { }

    public AuditEvent(Instant occurredAt, String actor, String incidentId, String action, String detail,
                      String previousStatus, String newStatus) {
        this.id = UUID.randomUUID().toString();
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.incidentId = incidentId == null ? SYSTEM_SCOPE : incidentId;
        this.action = action;
        this.detail = detail == null || detail.length() <= 4000 ? detail : detail.substring(0, 4000);
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public String getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActor() { return actor; }
    public String getIncidentId() { return incidentId; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
}
