package com.resolveit.escalation;

import com.resolveit.domain.sla.Escalation;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventPublisher;
import com.resolveit.event.IncidentEventType;
import com.resolveit.repository.EscalationRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Persists one escalation step and publishes the ESCALATED event (audited and notified by the observers). */
@Component
public class EscalationRecorder {
    private final EscalationRepository escalations;
    private final IncidentEventPublisher events;

    public EscalationRecorder(EscalationRepository escalations, IncidentEventPublisher events) {
        this.escalations = escalations;
        this.events = events;
    }

    public void record(EscalationRequest request, int level, String targetRole, List<? extends PortalUser> recipients) {
        var incident = request.incident();
        if (!incident.escalateTo(level) || escalations.existsByIncidentAndLevel(incident, level)) return;
        String names = recipients.isEmpty() ? "nobody available" : recipients.stream().map(PortalUser::getName).collect(Collectors.joining(", "));
        String reason = "Level " + level + " escalation to " + targetRole.toLowerCase().replace('_', ' ') + " (" + names + "): "
                + incident.getPriority() + " " + incident.getPublicId() + " not acknowledged " + request.minutesOverdue()
                + " min after its response deadline.";
        escalations.save(new Escalation(incident, level, targetRole,
                recipients.isEmpty() ? null : recipients.get(0).getId(), reason, request.at()));
        events.publish(IncidentEvent.of(IncidentEventType.ESCALATED, incident, IncidentEvent.SYSTEM, reason, null, request.at())
                .with("recipients", recipients.stream().map(PortalUser::getId).collect(Collectors.joining(",")))
                .with("level", String.valueOf(level)));
    }
}
