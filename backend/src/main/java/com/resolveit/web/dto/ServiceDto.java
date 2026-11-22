package com.resolveit.web.dto;

import java.util.List;

/** IT service catalog entry (technicians only in the detail view). */
public record ServiceDto(Long id, String name, String description, String category, int criticality, String keywords, boolean active,
        long technicianCount, long incidentCount, List<UserDto> technicians) { }
