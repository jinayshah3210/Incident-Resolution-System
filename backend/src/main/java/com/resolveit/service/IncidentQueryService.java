package com.resolveit.service;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.sla.SlaStatus;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.exception.ValidationException;
import com.resolveit.repository.EscalationRepository;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.repository.IncidentSpecifications;
import com.resolveit.repository.SlaViolationRepository;
import com.resolveit.web.dto.IncidentDetailDto;
import com.resolveit.web.dto.IncidentDto;
import com.resolveit.web.dto.IncidentSearchCriteria;
import com.resolveit.web.mapper.IncidentMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side: role-scoped lists, the detail view and the multi-criteria search (JPA Specifications). */
@Service
public class IncidentQueryService {
    private final IncidentRepository incidents;
    private final IncidentWorkflowService workflow;
    private final AuditService audit;
    private final IncidentCorrelationService correlation;
    private final SlaViolationRepository violations;
    private final EscalationRepository escalations;
    private final IncidentMapper mapper;

    public IncidentQueryService(IncidentRepository incidents, IncidentWorkflowService workflow, AuditService audit,
                                IncidentCorrelationService correlation, SlaViolationRepository violations,
                                EscalationRepository escalations, IncidentMapper mapper) {
        this.incidents = incidents;
        this.workflow = workflow;
        this.audit = audit;
        this.correlation = correlation;
        this.violations = violations;
        this.escalations = escalations;
        this.mapper = mapper;
    }

    /** scope: ongoing (not CLOSED), past (CLOSED) or all. Employees see their reports, technicians their assignments. */
    @Transactional(readOnly = true)
    public List<IncidentDto> list(PortalUser user, String scope) {
        List<Incident> source = switch (user.getRole()) {
            case EMPLOYEE -> incidents.findByReporterIdOrderByCreatedAtDesc(user.getId());
            case TECHNICIAN -> incidents.findByTechnicianIdOrderByCreatedAtDesc(user.getId());
            case ADMIN -> incidents.findAllByOrderByCreatedAtDesc();
        };
        return mapper.toDtos(source.stream().filter(i -> inScope(i, scope)).toList());
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> offers(PortalUser technician) {
        AccessGuard.requireRole(technician, Role.TECHNICIAN);
        return mapper.toDtos(incidents.findByTechnicianIdOrderByCreatedAtDesc(technician.getId()).stream()
                .filter(i -> i.getStatus() == IncidentStatus.ASSIGNED).toList());
    }

    @Transactional(readOnly = true)
    public IncidentDetailDto detail(PortalUser user, String id) {
        Incident incident = workflow.find(id);
        AccessGuard.requireView(user, incident);
        return new IncidentDetailDto(mapper.toDto(incident), audit.forIncident(incident.getPublicId()),
                correlation.related(incident).stream().map(mapper::related).toList(),
                violations.findByIncident(incident).stream().map(mapper::violation).toList(),
                escalations.findByIncidentOrderByLevelAsc(incident).stream().map(mapper::escalation).toList());
    }

    /** Role scoping is automatic and cannot be widened by request parameters. */
    @Transactional(readOnly = true)
    public List<IncidentDto> search(PortalUser user, IncidentSearchCriteria c) {
        Specification<Incident> spec = switch (user.getRole()) {
            case EMPLOYEE -> IncidentSpecifications.reportedBy(user.getId());
            case TECHNICIAN -> IncidentSpecifications.assignedTo(user.getId());
            case ADMIN -> IncidentSpecifications.all();
        };
        if (has(c.q())) spec = spec.and(IncidentSpecifications.text(c.q()));
        if (has(c.status())) spec = spec.and(IncidentSpecifications.status(parse(IncidentStatus.class, c.status(), "status")));
        if (has(c.priority())) spec = spec.and(IncidentSpecifications.priority(parse(Priority.class, c.priority(), "priority")));
        if (has(c.category())) spec = spec.and(IncidentSpecifications.category(c.category()));
        if (has(c.service())) spec = spec.and(IncidentSpecifications.service(c.service()));
        if (has(c.technician())) spec = spec.and(IncidentSpecifications.technician(c.technician()));
        if (has(c.slaStatus())) spec = spec.and(IncidentSpecifications.slaStatus(SlaStatus.parse(c.slaStatus())));
        if (c.from() != null) spec = spec.and(IncidentSpecifications.createdFrom(c.from()));
        if (c.to() != null) spec = spec.and(IncidentSpecifications.createdTo(c.to()));
        if (c.from() != null && c.to() != null && c.from().isAfter(c.to())) throw ValidationException.field("from", "The start date must be before the end date.");
        if ("ongoing".equalsIgnoreCase(c.scope())) spec = spec.and(IncidentSpecifications.open(true));
        if ("past".equalsIgnoreCase(c.scope())) spec = spec.and(IncidentSpecifications.open(false));
        return mapper.toDtos(incidents.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private static boolean inScope(Incident i, String scope) {
        if (scope == null || scope.equalsIgnoreCase("all")) return true;
        return scope.equalsIgnoreCase("past") ? i.getStatus() == IncidentStatus.CLOSED : i.getStatus() != IncidentStatus.CLOSED;
    }

    private static boolean has(String value) { return value != null && !value.isBlank(); }

    private static <E extends Enum<E>> E parse(Class<E> type, String raw, String field) {
        try {
            return Enum.valueOf(type, raw.strip().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw ValidationException.field(field, "Unknown " + field + ": " + raw);
        }
    }
}
