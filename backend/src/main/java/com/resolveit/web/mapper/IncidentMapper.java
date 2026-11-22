package com.resolveit.web.mapper;

import com.resolveit.domain.audit.AuditEvent;
import com.resolveit.domain.incident.ChatMessage;
import com.resolveit.domain.incident.Feedback;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentNote;
import com.resolveit.domain.sla.Escalation;
import com.resolveit.domain.sla.SlaViolation;
import com.resolveit.repository.FeedbackRepository;
import com.resolveit.strategy.priority.WeightedPriorityStrategy;
import com.resolveit.web.dto.AuditEventDto;
import com.resolveit.web.dto.ChatMessageDto;
import com.resolveit.web.dto.EscalationDto;
import com.resolveit.web.dto.IncidentDto;
import com.resolveit.web.dto.NoteDto;
import com.resolveit.web.dto.RelatedIncidentDto;
import com.resolveit.web.dto.SlaViolationDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Maps incident aggregates to API records. Must run inside a transaction (notes are lazy). */
@Component
public class IncidentMapper {
    private final FeedbackRepository feedback;

    public IncidentMapper(FeedbackRepository feedback) { this.feedback = feedback; }

    public IncidentDto toDto(Incident i) {
        return toDto(i, feedback.findByIncident(i).orElse(null));
    }

    public List<IncidentDto> toDtos(Collection<Incident> incidents) {
        if (incidents.isEmpty()) return List.of();
        Map<Long, Feedback> byIncident = feedback.findByIncidentIn(incidents).stream()
                .collect(Collectors.toMap(f -> f.getIncident().getId(), Function.identity(), (a, b) -> a));
        return incidents.stream().map(i -> toDto(i, byIncident.get(i.getId()))).toList();
    }

    public IncidentDto toDto(Incident i, Feedback rating) {
        int max = WeightedPriorityStrategy.KEY.equals(i.getPriorityStrategy()) ? 100 : 64;
        return new IncidentDto(i.getId(), i.getPublicId(), i.getTitle(), i.getDescription(), i.getReporterId(),
                i.getReporterName(), i.getTechnicianId(), i.getTechnicianName(), i.getCategory(),
                i.getClassificationConfidence(), i.getServiceName(), i.getImpact(), i.getUrgency(), i.getCriticality(),
                i.getAffectedUsers(), i.getPriorityScore(), max, name(i.getPriority()), i.getPriorityStrategy(),
                name(i.getStatus()), i.getCreatedAt(), i.getClassifiedAt(), i.getAssignedAt(), i.getAcknowledgedAt(),
                i.getStartedAt(), i.getResolvedAt(), i.getClosedAt(), i.getResponseDueAt(), i.getResolutionDueAt(),
                name(i.getResponseSla()), name(i.getResolutionSla()), name(i.getSlaStatus()), i.isSlaViolated(),
                i.isResponseSlaViolated(), i.isResolutionSlaViolated(), i.getEscalationLevel(), i.getResolutionSummary(),
                i.getRootCause(), i.getResolutionSteps(), i.isTechnicianApproved(), i.isEmployeeApproved(),
                i.getNotesTranscript(), i.getNotes().stream().map(this::note).toList(), i.getCorrelationGroup(),
                i.getDuplicateOf(), rating == null ? null : rating.getRating(), rating == null ? null : rating.getComment());
    }

    public NoteDto note(IncidentNote n) {
        return new NoteDto(n.getId(), n.getType().name(), n.getAuthorId(), n.getAuthorName(), n.getContent(), n.getCreatedAt());
    }

    public ChatMessageDto chat(ChatMessage m) {
        return new ChatMessageDto(m.getId(), m.getSentAt(), m.getSenderId(), m.getSenderName(), m.getContent());
    }

    public AuditEventDto audit(AuditEvent a) {
        return new AuditEventDto(a.getId(), a.getOccurredAt(), a.getActor(), a.getIncidentId(), a.getAction(), a.getDetail(),
                a.getPreviousStatus(), a.getNewStatus());
    }

    public RelatedIncidentDto related(Incident i) {
        return new RelatedIncidentDto(i.getPublicId(), i.getTitle(), i.getReporterName(), name(i.getStatus()),
                name(i.getPriority()), i.getCreatedAt());
    }

    public SlaViolationDto violation(SlaViolation v) {
        return new SlaViolationDto(v.getId(), v.getIncident().getPublicId(), v.getIncident().getTitle(), v.getType().name(),
                v.getDueAt(), v.getDetectedAt(), v.getDelayMinutes(), v.getTechnicianId(), v.isDismissed());
    }

    public EscalationDto escalation(Escalation e) {
        return new EscalationDto(e.getLevel(), e.getTargetRole(), e.getTargetUser(), e.getReason(), e.getCreatedAt());
    }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }
}
