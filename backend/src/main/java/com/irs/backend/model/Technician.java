
package com.irs.backend.model;

import java.util.List;
import java.util.ArrayList;

public class Technician extends User {

    private String department;
    private List<String> skills;
    private boolean available;
    private int activeIncidentCount;

    public Technician(
            String name,
            String email,
            String passwordHash,
            String department) {

        super(name, email, passwordHash);

        this.department = department;
        this.skills = new ArrayList<>();
        this.available = true;
        this.activeIncidentCount = 0;
    }

    @Override
    public String getRole() {
        return "TECHNICIAN";
    }

    // Getters and Setters

    public String getDepartment() {
        return department;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = new ArrayList<>(skills);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getActiveIncidentCount() {
        return activeIncidentCount;
    }

    public void setActiveIncidentCount(int activeIncidentCount) {
        if (activeIncidentCount < 0) {
            throw new IllegalArgumentException(
                "Incident count cannot be negative"
            );
        }

        this.activeIncidentCount = activeIncidentCount;
    }
}
