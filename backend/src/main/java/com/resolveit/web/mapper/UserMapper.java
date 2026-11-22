package com.resolveit.web.mapper;

import com.resolveit.domain.user.Administrator;
import com.resolveit.domain.user.Employee;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Technician;
import com.resolveit.web.dto.UserDto;
import org.springframework.stereotype.Component;

/** Maps accounts to UserDto using pattern matching on the concrete subclass; never exposes the password hash. */
@Component
public class UserMapper {
    public UserDto toDto(PortalUser user) { return toDto(user, null, null, null); }

    public UserDto toDto(PortalUser user, Long workload, Long reported, Long resolved) {
        if (user instanceof Technician t) {
            long load = workload == null ? 0 : workload;
            String availability = t.isActive() && t.hasCapacity(load) ? "Available" : "Unavailable";
            return new UserDto(t.getId(), t.getName(), t.getEmail(), t.getRole().name(), t.getDepartment(), t.getPhone(),
                    t.isActive(), t.getSkills(), t.getMaxWorkload(), load, availability, t.getExperienceYears(),
                    t.isTeamLead(), null, null, null, resolved);
        }
        if (user instanceof Administrator a) {
            return new UserDto(a.getId(), a.getName(), a.getEmail(), a.getRole().name(), a.getDepartment(), a.getPhone(),
                    a.isActive(), null, null, null, null, null, null, a.getAdminLevel().name(), null, null, null);
        }
        Employee e = (Employee) user;
        return new UserDto(e.getId(), e.getName(), e.getEmail(), e.getRole().name(), e.getDepartment(), e.getPhone(),
                e.isActive(), null, null, null, null, null, null, null, e.getJoinDate(), reported, resolved);
    }
}
