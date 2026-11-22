package com.resolveit.web.dto;

import java.time.Instant;

/** One escalation step. */
public record EscalationDto(int level, String targetRole, String targetUser, String reason, Instant createdAt) { }
