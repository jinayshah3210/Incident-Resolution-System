package com.resolveit.web.dto;

import java.time.Instant;

/** The eight-plus filters of GET /api/incidents/search. */
public record IncidentSearchCriteria(String q, String status, String priority, String category, String service, String technician, String slaStatus,
        Instant from, Instant to, String scope) { }
