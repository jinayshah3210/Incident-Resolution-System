package com.resolveit.web.dto;

import java.time.Instant;

/** A recorded SLA breach. */
public record SlaViolationDto(Long id, String incidentId, String incidentTitle, String type, Instant dueAt, Instant detectedAt, long delayMinutes,
        String technicianId, boolean dismissed) { }
