package com.resolveit.domain.incident;

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

/** One message of the incident-scoped conversation (composition: created only through Incident.addChatMessage). */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;
    @Column(name = "sender_id", nullable = false, length = 20)
    private String senderId;
    @Column(name = "sender_name", length = 120)
    private String senderName;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected ChatMessage() { }

    ChatMessage(Incident incident, String senderId, String senderName, String content, Instant sentAt) {
        this.incident = incident;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.sentAt = sentAt;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public Instant getSentAt() { return sentAt; }
}
