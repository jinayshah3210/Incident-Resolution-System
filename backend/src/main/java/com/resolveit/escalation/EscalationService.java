package com.resolveit.escalation;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.Priority;
import com.resolveit.service.SettingsService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Automatic escalation: a P1/P2 incident not acknowledged by its response deadline climbs the chain
 * technician -> team lead -> admin -> IT manager, one level per elapsed response window. Idempotent.
 */
@Service
public class EscalationService {
    private final EscalationHandler chain;
    private final SettingsService settings;

    public EscalationService(TechnicianReminderHandler technician, TeamLeadHandler teamLead, AdminHandler admin,
                             ItManagerHandler itManager, SettingsService settings) {
        technician.linkWith(teamLead).linkWith(admin).linkWith(itManager);
        this.chain = technician;
        this.settings = settings;
    }

    public void evaluate(Incident incident, Instant now) {
        if (!settings.escalationEnabled()) return;
        if (incident.getPriority() == null || !incident.getPriority().isAtLeast(Priority.P2)) return;
        if (incident.getStatus() != IncidentStatus.CLASSIFIED && incident.getStatus() != IncidentStatus.ASSIGNED) return;
        Instant due = incident.getResponseDueAt();
        if (due == null || !now.isAfter(due)) return;
        long window = Math.max(1, Duration.between(incident.getCreatedAt(), due).toSeconds());
        long overdue = Duration.between(due, now).toSeconds();
        int target = (int) Math.min(4, 1 + overdue / window);
        chain.handle(new EscalationRequest(incident, target, overdue / 60, now));
    }

    public EscalationHandler chain() { return chain; }
}
