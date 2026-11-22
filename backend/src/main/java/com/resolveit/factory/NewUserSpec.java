package com.resolveit.factory;

import com.resolveit.domain.user.AdminLevel;
import java.time.LocalDate;
import java.util.Set;

/** Everything needed to create any kind of account through UserFactory. */
public record NewUserSpec(String id, String name, String email, String rawPassword, String department,
                          Set<String> skills, int maxWorkload, int experienceYears, boolean teamLead,
                          AdminLevel adminLevel, LocalDate joinDate) {
    public static NewUserSpec employee(String id, String name, String email, String password, String department) {
        return new NewUserSpec(id, name, email, password, department, Set.of(), 0, 0, false, null, LocalDate.now());
    }

    public static NewUserSpec technician(String id, String name, String email, String password, String department,
                                         Set<String> skills, int maxWorkload, int experienceYears, boolean teamLead) {
        return new NewUserSpec(id, name, email, password, department, skills, maxWorkload, experienceYears, teamLead, null, null);
    }

    public static NewUserSpec admin(String id, String name, String email, String password, AdminLevel level) {
        return new NewUserSpec(id, name, email, password, null, Set.of(), 0, 0, false, level, null);
    }
}
