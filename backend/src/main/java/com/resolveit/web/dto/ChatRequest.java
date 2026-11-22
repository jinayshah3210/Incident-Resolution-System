package com.resolveit.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** New chat message. */
public record ChatRequest(@NotBlank(message = "Write a message first.") @Size(max = 2000) String content) { }
