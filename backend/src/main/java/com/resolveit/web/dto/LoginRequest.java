package com.resolveit.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Login body. The portal role must match the account. */
public record LoginRequest(@NotBlank(message = "Enter your email or ID.") String login,
        @NotBlank(message = "Enter your password.") String password,
        @NotBlank(message = "Choose a portal.") String role) { }
