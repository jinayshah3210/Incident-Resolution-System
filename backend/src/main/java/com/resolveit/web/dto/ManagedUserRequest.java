package com.resolveit.web.dto;

import java.util.List;

/** Admin create/update of an employee or technician. */
public record ManagedUserRequest(String role, String name, String email, String password, String department, String phone, List<String> skills,
        Integer maxWorkload, Integer experienceYears, Boolean teamLead) { }
