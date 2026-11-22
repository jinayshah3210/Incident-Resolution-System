package com.resolveit.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-registration body; always creates an EMPLOYEE (any role field is ignored). */
public record RegisterRequest(@NotBlank(message = "Name is required.") @Size(max = 120) String name,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email.") String email,
        @NotBlank @Size(min = 8, max = 100, message = "Password must have at least 8 characters.") String password,
        @Size(max = 120) String department) { }
