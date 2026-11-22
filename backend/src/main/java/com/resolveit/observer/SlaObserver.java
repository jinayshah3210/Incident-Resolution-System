package com.resolveit.observer;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.sla.SlaViolation;
import com.resolveit.domain.sla.ViolationType;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventType;
import com.resolveit.event.IncidentObserver;
import com.resolveit.repository.SlaViolationRepository;
import org.springframework.stereotype.Component;

/** Observer pattern: records a violation row the first time each SLA clock is breached (idempotent). */
@Component
public class SlaObserver implements IncidentObserver {
    private final SlaViolationRepository violations;

    public SlaObserver(SlaViolationRepository violations) { this.violations = violations; }

    @Override public int order() { return 10; }

    @Override
    public void onEvent(IncidentEvent event) {
        ViolationType type = event.type() == IncidentEventType.RESPONSE_SLA_VIOLATED ? ViolationType.RESPONSE
                : event.type() == IncidentEventType.RESOLUTION_SLA_VIOLATED ? ViolationType.RESOLUTION : null;
        if (type == null) return;
        Incident incident = event.incident();
        if (incident.getId() == null || violations.existsByIncidentAndType(incident, type)) return;
        violations.save(new SlaViolation(incident, type, incident.dueAt(type), event.occurredAt()));
    }
}
