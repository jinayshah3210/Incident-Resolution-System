package com.resolveit.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** New incident body. affectedUsers: 1 just me, 2 a few people, 3 a team/department, 4 everyone. */
public record CreateIncidentRequest(@NotBlank(message = "Title is required.") @Size(min = 5, max = 120, message = "Title must be between 5 and 120 characters.") String title,
        @NotBlank(message = "Description is required.") @Size(min = 10, max = 2000, message = "Description must be between 10 and 2,000 characters.") String description,
        @Size(max = 120) String service,
        @Min(value = 1, message = "Choose 1-4.") @Max(value = 4, message = "Choose 1-4.") Integer affectedUsers,
        Boolean force) { }
