package com.resolveit.escalation;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.repository.TechnicianRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Chain of Responsibility, level 1: remind the technician holding the offer. */
@Component
public class TechnicianReminderHandler extends EscalationHandler {
    private final TechnicianRepository technicians;

    public TechnicianReminderHandler(EscalationRecorder recorder, TechnicianRepository technicians) {
        super(recorder);
        this.technicians = technicians;
    }

    @Override public int level() { return 1; }
    @Override public String targetRole() { return "TECHNICIAN"; }

    @Override
    protected List<? extends PortalUser> recipients(Incident incident) {
        if (incident.getTechnicianId() == null) return List.of();
        return technicians.findById(incident.getTechnicianId()).map(List::of).orElse(List.of());
    }
}
