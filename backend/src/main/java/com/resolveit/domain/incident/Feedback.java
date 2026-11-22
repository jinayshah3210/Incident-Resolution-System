package com.resolveit.domain.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Employee satisfaction rating (1-5) for a resolved incident. Created only by Incident.leaveFeedback. */
@Entity
@Table(name = "feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;
    @Column(name = "employee_id", nullable = false, length = 20)
    private String employeeId;
    @Column(nullable = false)
    private int rating;
    @Column(length = 1000)
    private String comment;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Feedback() { }

    Feedback(Incident incident, String employeeId, int rating, String comment, Instant createdAt) {
        this.incident = incident;
        this.employeeId = employeeId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public String getEmployeeId() { return employeeId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
