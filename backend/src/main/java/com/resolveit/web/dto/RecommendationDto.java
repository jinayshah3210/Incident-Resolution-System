package com.resolveit.web.dto;


/** Smart technician recommendation with every component of the score visible. */
public record RecommendationDto(String technicianId, String name, double score, double skillMatch, double loadScore, double experienceScore,
        double historyScore, long load, int maxWorkload, int experienceYears, long pastIncidentsOnService, boolean eligible,
        String note) { }
