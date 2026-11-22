package com.resolveit.web.dto;

import java.time.LocalDate;
import java.util.Set;

/** Account view; never contains the password hash. */
public record UserDto(String id, String name, String email, String role, String department, String phone, Boolean active,
        Set<String> skills, Integer maxWorkload, Long workload, String availability, Integer experienceYears,
        Boolean teamLead, String adminLevel, LocalDate joinDate, Long incidentsReported, Long incidentsResolved) { }
