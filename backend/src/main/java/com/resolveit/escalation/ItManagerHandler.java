package com.resolveit.escalation;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.AdminLevel;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.repository.AdministratorRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Chain of Responsibility, level 4 (end of chain): the IT manager. */
@Component
public class ItManagerHandler extends EscalationHandler {
    private final AdministratorRepository administrators;

    public ItManagerHandler(EscalationRecorder recorder, AdministratorRepository administrators) {
        super(recorder);
        this.administrators = administrators;
    }

    @Override public int level() { return 4; }
    @Override public String targetRole() { return "IT_MANAGER"; }

    @Override
    protected List<? extends PortalUser> recipients(Incident incident) {
        return administrators.findByAdminLevelAndActiveTrueOrderByIdAsc(AdminLevel.IT_MANAGER);
    }
}
