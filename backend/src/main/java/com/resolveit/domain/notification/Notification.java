package com.resolveit.domain.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A message for one recipient. Built by NotificationFactory and delivered through every NotificationChannel. */
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;
    @Column(name = "incident_id", length = 20)
    private String incidentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType type;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected Notification() { }

    public Notification(String userId, String incidentId, NotificationType type, String title, String message, Instant createdAt) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.incidentId = incidentId;
        this.type = type;
        this.title = title;
        this.message = message.length() > 1000 ? message.substring(0, 1000) : message;
        this.createdAt = createdAt;
    }

    public void markRead() { this.read = true; }

    public boolean belongsTo(String user) { return userId.equals(user); }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getIncidentId() { return incidentId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
}
