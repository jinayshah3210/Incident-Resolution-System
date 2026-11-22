package com.resolveit.web.dto;

import java.time.Instant;
import java.util.Map;

/** Uniform error body for every failure. */
public record ApiError(Instant timestamp, int status, String code, String message, String path, Map<String, String> fieldErrors,
        Map<String, Object> details, String correlationId) { }
