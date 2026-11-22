package com.resolveit.domain.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One email produced by the EmailChannel adapter. */
@Entity
@Table(name = "email_outbox")
public class EmailOutboxEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_email", nullable = false, length = 160)
    private String recipientEmail;
    @Column(nullable = false, length = 200)
    private String subject;
    @Column(nullable = false, length = 2000)
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EmailStatus status;
    @Column(length = 300)
    private String error;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailOutboxEntry() { }

    public EmailOutboxEntry(String recipientEmail, String subject, String body, EmailStatus status, String error, Instant createdAt) {
        this.recipientEmail = recipientEmail;
        this.subject = subject.length() > 200 ? subject.substring(0, 200) : subject;
        this.body = body.length() > 2000 ? body.substring(0, 2000) : body;
        this.status = status;
        this.error = error == null || error.length() <= 300 ? error : error.substring(0, 300);
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public EmailStatus getStatus() { return status; }
    public String getError() { return error; }
    public Instant getCreatedAt() { return createdAt; }
}
