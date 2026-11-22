package com.resolveit.escalation;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.AdminLevel;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.repository.AdministratorRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Chain of Responsibility, level 3: the service-desk administrators. */
@Component
public class AdminHandler extends EscalationHandler {
    private final AdministratorRepository administrators;

    public AdminHandler(EscalationRecorder recorder, AdministratorRepository administrators) {
        super(recorder);
        this.administrators = administrators;
    }

    @Override public int level() { return 3; }
    @Override public String targetRole() { return "ADMIN"; }

    @Override
    protected List<? extends PortalUser> recipients(Incident incident) {
        return administrators.findByAdminLevelAndActiveTrueOrderByIdAsc(AdminLevel.ADMINISTRATOR);
    }
}
