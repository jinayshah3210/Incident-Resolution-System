package com.resolveit.web.dto;

import java.time.Instant;
import java.util.List;

/** Incident as returned by the API (superset of the original JSON shape). */
public record IncidentDto(Long id, String publicId, String title, String description, String reporterId, String reporterName,
        String technicianId, String technicianName, String category, int classificationConfidence, String serviceName,
        int impact, int urgency, int criticality, int affectedUsers, int priorityScore, int priorityMaxScore,
        String priority, String priorityStrategy, String status, Instant createdAt, Instant classifiedAt,
        Instant assignedAt, Instant acknowledgedAt, Instant startedAt, Instant resolvedAt, Instant closedAt,
        Instant responseDueAt, Instant resolutionDueAt, String responseSla, String resolutionSla, String slaStatus,
        boolean slaViolated, boolean responseSlaViolated, boolean resolutionSlaViolated, int escalationLevel,
        String resolutionSummary, String rootCause, String resolutionSteps, boolean technicianApproved,
        boolean employeeApproved, String notes, List<NoteDto> noteEntries, String correlationGroup, String duplicateOf,
        Integer feedbackRating, String feedbackComment) { }
