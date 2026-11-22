package com.resolveit.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin manual reassignment. */
public record AssignRequest(@NotBlank(message = "Choose a technician.") String technicianId) { }
