package com.resolveit.web.dto;

import java.time.Instant;

/** One bell-panel notification. */
public record NotificationDto(String id, String userId, String incidentId, String type, String title, String message, Instant createdAt, boolean read) { }
