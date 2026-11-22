package com.resolveit.web.dto;

import java.time.Instant;

/** One audit row. */
public record AuditEventDto(String id, Instant at, String actor, String incidentId, String action, String detail, String previousStatus, String newStatus) { }
