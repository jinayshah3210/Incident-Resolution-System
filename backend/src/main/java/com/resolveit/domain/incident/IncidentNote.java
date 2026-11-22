package com.resolveit.domain.incident;

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
import java.time.Instant;

/** A note owned by an incident (composition: created only through Incident.addNote). */
@Entity
@Table(name = "incident_notes")
public class IncidentNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;
    @Column(name = "author_id", nullable = false, length = 20)
    private String authorId;
    @Column(name = "author_name", length = 120)
    private String authorName;
    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 30)
    private NoteType type;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IncidentNote() { }

    IncidentNote(Incident incident, String authorId, String authorName, NoteType type, String content, Instant createdAt) {
        this.incident = incident;
        this.authorId = authorId;
        this.authorName = authorName;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public NoteType getType() { return type; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
