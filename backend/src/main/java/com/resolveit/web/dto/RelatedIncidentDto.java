package com.resolveit.web.dto;

import java.time.Instant;

/** An incident in the same correlation group. */
public record RelatedIncidentDto(String publicId, String title, String reporterName, String status, String priority, Instant createdAt) { }
