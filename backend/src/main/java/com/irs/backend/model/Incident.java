
package com.irs.backend.model;

import com.irs.backend.model.enums.*;
import java.time.LocalDateTime;

public class Incident {

    private Long id;

    private String title;
    private String description;
    private String category;

    private Impact impact;
    private Urgency urgency;
    private Priority priority;
    private IncidentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime responseDeadline;
    private LocalDateTime resolutionDeadline;
    private LocalDateTime respondedAt;

    // Constructor
    public Incident(String title, String description,String category) {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.title = title;
        this.description = description;
        this.category = category;

        this.status = IncidentStatus.NEW;
        
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    // Setters
    public void setImpact(Impact impact) {
        this.impact = impact;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public Impact getImpact() {
        return impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResponseDeadline() {
        return responseDeadline;
    }

    public void setResponseDeadline(LocalDateTime responseDeadline) {
        this.responseDeadline = responseDeadline;
    }

    public LocalDateTime getResolutionDeadline() {
        return resolutionDeadline;
    }

    public void setResolutionDeadline(LocalDateTime resolutionDeadline) {
        this.resolutionDeadline = resolutionDeadline;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
