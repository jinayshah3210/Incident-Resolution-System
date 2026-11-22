package com.resolveit.domain.user;

import com.resolveit.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** An IT administrator (or IT manager) with oversight of every incident. */
@Entity
@Table(name = "administrators")
@DiscriminatorValue("ADMIN")
@PrimaryKeyJoinColumn(name = "user_id")
public class Administrator extends PortalUser {
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_level", nullable = false, length = 20)
    private AdminLevel adminLevel = AdminLevel.ADMINISTRATOR;

    protected Administrator() { }

    public Administrator(String id, String name, String email, String rawPassword) {
        this(id, name, email, rawPassword, AdminLevel.ADMINISTRATOR);
    }

    public Administrator(String id, String name, String email, String rawPassword, AdminLevel level) {
        super(id, name, email, rawPassword, level == AdminLevel.IT_MANAGER ? "IT Management" : "IT Operations");
        this.adminLevel = level;
    }

    @Override public Role getRole() { return Role.ADMIN; }

    @Override public boolean canView(Incident incident) { return true; }

    public AdminLevel getAdminLevel() { return adminLevel; }

    public boolean isItManager() { return adminLevel == AdminLevel.IT_MANAGER; }
}
