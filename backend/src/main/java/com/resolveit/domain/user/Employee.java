package com.resolveit.domain.user;

import com.resolveit.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** An employee reports incidents and may only see the incidents they reported. */
@Entity
@Table(name = "employees")
@DiscriminatorValue("EMPLOYEE")
@PrimaryKeyJoinColumn(name = "user_id")
public class Employee extends PortalUser {
    @Column(name = "join_date")
    private LocalDate joinDate;

    protected Employee() { }

    public Employee(String id, String name, String email, String rawPassword, String department) {
        this(id, name, email, rawPassword, department, LocalDate.now());
    }

    public Employee(String id, String name, String email, String rawPassword, String department, LocalDate joinDate) {
        super(id, name, email, rawPassword, department);
        this.joinDate = joinDate;
    }

    @Override public Role getRole() { return Role.EMPLOYEE; }

    @Override public boolean canView(Incident incident) { return getId().equals(incident.getReporterId()); }

    public LocalDate getJoinDate() { return joinDate; }
}
