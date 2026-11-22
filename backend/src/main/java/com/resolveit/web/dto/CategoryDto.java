package com.resolveit.web.dto;

import java.util.Map;

/** Category with its classifier keywords and usage statistics. */
public record CategoryDto(Long id, String name, String description, boolean active, Map<String, Integer> keywords, long technicianCount,
        long openIncidents) { }
