package com.resolveit.web.dto;

import java.util.List;
import java.util.Map;

/** Per-technician report. */
public record TechnicianReportDto(UserDto technician, long active, long resolved, long slaViolations, double averageResolutionMinutes,
        double averageRating, Map<String, Long> priorities, List<IncidentDto> recent) { }
