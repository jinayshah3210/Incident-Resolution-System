package com.resolveit.web.dto;

import java.util.List;
import java.util.Map;

/** Analytics for a period (all time or one month). */
public record ReportDto(String period, long total, long open, long resolved, long closed, long critical, long slaViolations,
        double slaCompliance, double averageResponseMinutes, double averageResolutionMinutes, double averageRating,
        long feedbackCount, long escalations, Map<String, Long> statuses, Map<String, Long> priorities,
        Map<String, Long> categories, Map<String, Long> services, Map<String, Long> technicians,
        List<DailyCount> trend) { }
