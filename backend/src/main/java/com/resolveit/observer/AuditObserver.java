package com.resolveit.observer;

import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentObserver;
import com.resolveit.service.AuditService;
import org.springframework.stereotype.Component;

/** Observer pattern: writes one append-only audit row for every incident event. */
@Component
public class AuditObserver implements IncidentObserver {
    private final AuditService audit;

    public AuditObserver(AuditService audit) { this.audit = audit; }

    @Override public int order() { return 20; }

    @Override public void onEvent(IncidentEvent event) { audit.record(event); }
}
