package com.resolveit.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin lifecycle override. */
public record AdvanceRequest(@NotBlank(message = "Choose a target status.") String status) { }
