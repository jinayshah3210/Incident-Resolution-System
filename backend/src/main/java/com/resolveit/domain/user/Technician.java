package com.resolveit.domain.user;

import com.resolveit.domain.incident.Incident;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * A support engineer. Aggregation: a technician holds a set of skill category names.
 * Business invariant: never more than maxWorkload active incidents (see hasCapacity).
 */
@Entity
@Table(name = "technicians")
@DiscriminatorValue("TECHNICIAN")
@PrimaryKeyJoinColumn(name = "user_id")
public class Technician extends PortalUser {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "skill", length = 60)
    private Set<String> skills = new TreeSet<>();
    @Column(name = "max_workload", nullable = false)
    private int maxWorkload;
    @Column(name = "experience_years", nullable = false)
    private int experienceYears;
    @Column(name = "team_lead", nullable = false)
    private boolean teamLead;

    protected Technician() { }

    public Technician(String id, String name, String email, String rawPassword, String department,
                      Collection<String> skills, int maxWorkload) {
        this(id, name, email, rawPassword, department, skills, maxWorkload, 3, false);
    }

    public Technician(String id, String name, String email, String rawPassword, String department,
                      Collection<String> skills, int maxWorkload, int experienceYears, boolean teamLead) {
        super(id, name, email, rawPassword, department);
        this.skills = new TreeSet<>(skills);
        this.maxWorkload = maxWorkload;
        this.experienceYears = experienceYears;
        this.teamLead = teamLead;
    }

    @Override public Role getRole() { return Role.TECHNICIAN; }

    /** A technician sees incidents currently offered or assigned to them. */
    @Override public boolean canView(Incident incident) { return getId().equals(incident.getTechnicianId()); }

    public boolean canHandle(String category) {
        return category != null && skills.stream().anyMatch(skill -> skill.equalsIgnoreCase(category));
    }

    public boolean hasCapacity(long currentLoad) { return currentLoad < maxWorkload; }

    public void updateProfile(Collection<String> newSkills, int newMaxWorkload, int newExperienceYears) {
        this.skills = new TreeSet<>(newSkills);
        this.maxWorkload = newMaxWorkload;
        this.experienceYears = newExperienceYears;
    }

    public void promoteToTeamLead() { this.teamLead = true; }

    public Set<String> getSkills() { return Collections.unmodifiableSet(skills); }
    public int getMaxWorkload() { return maxWorkload; }
    public int getExperienceYears() { return experienceYears; }
    public boolean isTeamLead() { return teamLead; }
}
