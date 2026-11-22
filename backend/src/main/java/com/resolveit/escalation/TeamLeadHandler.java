package com.resolveit.escalation;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.repository.TechnicianRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Chain of Responsibility, level 2: the support team leads. */
@Component
public class TeamLeadHandler extends EscalationHandler {
    private final TechnicianRepository technicians;

    public TeamLeadHandler(EscalationRecorder recorder, TechnicianRepository technicians) {
        super(recorder);
        this.technicians = technicians;
    }

    @Override public int level() { return 2; }
    @Override public String targetRole() { return "TEAM_LEAD"; }

    @Override
    protected List<? extends PortalUser> recipients(Incident incident) { return technicians.findByTeamLeadTrueAndActiveTrue(); }
}
