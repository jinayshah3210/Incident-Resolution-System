package com.resolveit.factory;

import com.resolveit.domain.user.AdminLevel;
import com.resolveit.domain.user.Administrator;
import com.resolveit.domain.user.Employee;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.domain.user.Technician;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: creates the right PortalUser subclass for a role. Creators live in an EnumMap,
 * so there is no role switch statement anywhere in the code base.
 */
@Component
public class UserFactory {
    private final Map<Role, Function<NewUserSpec, PortalUser>> creators = new EnumMap<>(Role.class);

    public UserFactory() {
        creators.put(Role.EMPLOYEE, s -> new Employee(s.id(), s.name(), s.email(), s.rawPassword(),
                s.department() == null || s.department().isBlank() ? "General" : s.department(),
                s.joinDate() == null ? LocalDate.now() : s.joinDate()));
        creators.put(Role.TECHNICIAN, s -> new Technician(s.id(), s.name(), s.email(), s.rawPassword(),
                s.department() == null || s.department().isBlank() ? "IT Support" : s.department(),
                s.skills(), s.maxWorkload(), s.experienceYears(), s.teamLead()));
        creators.put(Role.ADMIN, s -> new Administrator(s.id(), s.name(), s.email(), s.rawPassword(),
                s.adminLevel() == null ? AdminLevel.ADMINISTRATOR : s.adminLevel()));
    }

    public PortalUser create(Role role, NewUserSpec spec) {
        return creators.get(role).apply(spec);
    }
}
