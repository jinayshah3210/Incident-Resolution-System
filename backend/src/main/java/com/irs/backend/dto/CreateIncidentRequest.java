
package com.irs.backend.dto;

import com.irs.backend.model.enums.Impact;
import com.irs.backend.model.enums.Urgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateIncidentRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String category;

    @NotNull
    private Impact impact;

    @NotNull
    private Urgency urgency;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Impact getImpact() { return impact; }
    public Urgency getUrgency() { return urgency; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setImpact(Impact impact) { this.impact = impact; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
}
