package com.resolveit.web.dto;

import java.util.List;

/** Incident plus its history for the expanded view. */
public record IncidentDetailDto(IncidentDto incident, List<AuditEventDto> audit, List<RelatedIncidentDto> related, List<SlaViolationDto> violations,
        List<EscalationDto> escalations) { }
