package com.resolveit.web.dto;

import java.time.Instant;

/** Health check response. */
public record HealthDto(String status, String service, String database, Instant time) { }
