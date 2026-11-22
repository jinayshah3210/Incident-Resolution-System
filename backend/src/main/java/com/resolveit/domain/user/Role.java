package com.resolveit.domain.user;

import com.resolveit.exception.ValidationException;
import java.util.Locale;

/** The three portals of ResolveIT. Stored as the discriminator value of the users table. */
public enum Role {
    EMPLOYEE("Employee", "/employee"),
    TECHNICIAN("Technician", "/technician"),
    ADMIN("Administrator", "/admin");

    private final String label;
    private final String dashboardPath;

    Role(String label, String dashboardPath) {
        this.label = label;
        this.dashboardPath = dashboardPath;
    }

    public String getLabel() { return label; }
    public String getDashboardPath() { return dashboardPath; }

    public static Role parse(String raw) {
        if (raw == null) throw ValidationException.field("role", "A portal role is required.");
        try {
            return Role.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ValidationException.field("role", "Unknown portal role.");
        }
    }
}
