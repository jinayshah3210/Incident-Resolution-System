package com.resolveit.service;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.sla.SlaTargets;
import com.resolveit.domain.sla.SlaViolation;
import com.resolveit.domain.sla.ViolationType;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.escalation.EscalationService;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventPublisher;
import com.resolveit.event.IncidentEventType;
import com.resolveit.exception.MissingSlaConfigurationException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.repository.SlaViolationRepository;
import com.resolveit.web.dto.IncidentDto;
import com.resolveit.web.mapper.IncidentMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** SLA policy lookup (with safe fallback), deadline calculation, warnings, violations and the monitor loop. */
@Service
public class SlaService {
    private static final Logger log = LoggerFactory.getLogger(SlaService.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    /** Hard-coded defaults used when the policy table is missing a row. */
    public static final Map<Priority, SlaTargets> DEFAULTS = new EnumMap<>(Map.of(
            Priority.P1, new SlaTargets(5, 30, true), Priority.P2, new SlaTargets(15, 120, true),
            Priority.P3, new SlaTargets(60, 480, true), Priority.P4, new SlaTargets(240, 1440, true)));

    private final SettingsService settings;
    private final IncidentEventPublisher events;
    private final IncidentRepository incidents;
    private final SlaViolationRepository violations;
    private final EscalationService escalation;
    private final IncidentMapper mapper;
    private final Clock clock;
    private final TransactionTemplate perIncident;
    private final int timeScale;

    public SlaService(SettingsService settings, IncidentEventPublisher events, IncidentRepository incidents,
                      SlaViolationRepository violations, EscalationService escalation, IncidentMapper mapper, Clock clock,
                      PlatformTransactionManager transactions, @Value("${resolveit.sla.time-scale:1}") int timeScale) {
        this.settings = settings;
        this.events = events;
        this.incidents = incidents;
        this.violations = violations;
        this.escalation = escalation;
        this.mapper = mapper;
        this.clock = clock;
        this.perIncident = new TransactionTemplate(transactions);
        this.timeScale = Math.max(1, timeScale);
    }

    /** Policy lookup; a missing policy raises MissingSlaConfigurationException, which is caught and replaced by defaults. */
    public SlaTargets targetsFor(Priority priority) {
        try {
            return settings.slaTargets(priority).orElseThrow(
                    () -> new MissingSlaConfigurationException("No SLA policy is configured for " + priority + "."));
        } catch (MissingSlaConfigurationException missing) {
            log.warn("{} Using default targets.", missing.getMessage());
            return DEFAULTS.get(priority);
        }
    }

    /** Deadlines are always measured from createdAt, so recalculation after a priority change is consistent. */
    public void applyDeadlines(Incident incident, Instant at) {
        SlaTargets targets = targetsFor(incident.getPriority());
        incident.applySlaDeadlines(incident.getCreatedAt().plus(targets.response(timeScale)),
                incident.getCreatedAt().plus(targets.resolution(timeScale)));
        if (targets.fallback()) {
            events.publish(IncidentEvent.of(IncidentEventType.SLA_POLICY_FALLBACK, incident, IncidentEvent.SYSTEM,
                    "No SLA policy for " + incident.getPriority() + "; default targets " + targets.responseMinutes() + "/"
                            + targets.resolutionMinutes() + " min applied to " + incident.getPublicId() + ".", null, at));
        }
    }

    /** Evaluates both clocks once. Every transition is idempotent (state flags on the incident). */
    public void evaluate(Incident incident, Instant now) {
        for (ViolationType type : ViolationType.values()) {
            Instant due = incident.dueAt(type);
            if (due == null) continue;
            if (!incident.isClockRunning(type)) {
                Instant finished = type == ViolationType.RESPONSE ? incident.getAcknowledgedAt() : incident.getResolvedAt();
                if (finished != null && finished.isAfter(due) && incident.markViolated(type)) violated(incident, type, finished);
                continue;
            }
            if (now.isAfter(due)) {
                if (incident.markViolated(type)) violated(incident, type, now);
                continue;
            }
            long total = Math.max(1, Duration.between(incident.getCreatedAt(), due).toSeconds());
            long elapsed = Duration.between(incident.getCreatedAt(), now).toSeconds();
            if (elapsed * 100 >= total * settings.warningPercent() && incident.markApproaching(type)) {
                long remaining = Math.max(0, Duration.between(now, due).toMinutes());
                String clock = type == ViolationType.RESPONSE ? "response" : "resolution";
                events.publish(IncidentEvent.of(IncidentEventType.SLA_WARNING, incident, IncidentEvent.SYSTEM,
                        incident.getPublicId() + " has " + remaining + " min left on its " + clock + " target (due "
                                + TIME.format(due) + ").", null, now).with("clock", type.name()));
            }
        }
    }

    private void violated(Incident incident, ViolationType type, Instant at) {
        IncidentEventType eventType = type == ViolationType.RESPONSE ? IncidentEventType.RESPONSE_SLA_VIOLATED
                : IncidentEventType.RESOLUTION_SLA_VIOLATED;
        String clock = type == ViolationType.RESPONSE ? "Response" : "Resolution";
        events.publish(IncidentEvent.of(eventType, incident, IncidentEvent.SYSTEM,
                clock + " SLA deadline exceeded (due " + TIME.format(incident.dueAt(type)) + ").", null, at));
    }

    /** Scheduler entry point: each open incident is evaluated in its own transaction so one failure never stops the loop. */
    public int monitorOpenIncidents() {
        Instant now = clock.instant();
        List<Long> ids = incidents.findByStatusNot(IncidentStatus.CLOSED).stream().map(Incident::getId).toList();
        int processed = 0;
        for (Long id : ids) {
            try {
                perIncident.executeWithoutResult(tx -> incidents.findById(id).ifPresent(incident -> {
                    evaluate(incident, now);
                    escalation.evaluate(incident, now);
                }));
                processed++;
            } catch (RuntimeException failure) {
                log.warn("SLA monitor skipped incident {}: {}", id, failure.getMessage());
            }
        }
        return processed;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> openViolations(PortalUser actor) {
        AccessGuard.requireAdmin(actor);
        Map<Long, Map<String, Object>> byIncident = new LinkedHashMap<>();
        for (SlaViolation v : violations.findByDismissedFalseOrderByDetectedAtDesc()) {
            Map<String, Object> row = byIncident.computeIfAbsent(v.getIncident().getId(), id -> {
                Map<String, Object> m = new LinkedHashMap<>();
                IncidentDto dto = mapper.toDto(v.getIncident());
                m.put("incident", dto);
                m.put("violations", new java.util.ArrayList<>());
                return m;
            });
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) row.get("violations");
            list.add(mapper.violation(v));
        }
        return List.copyOf(byIncident.values());
    }

    /** Dismissing hides the alert; the violation rows are retained. Accepts an incident public id or a violation id. */
    @Transactional
    public Map<String, Object> dismiss(PortalUser actor, String id) {
        AccessGuard.requireAdmin(actor);
        List<SlaViolation> matched = incidents.findByPublicIdIgnoreCase(id).map(violations::findByIncident)
                .orElseGet(() -> {
                    try {
                        return violations.findById(Long.parseLong(id)).map(List::of).orElse(List.of());
                    } catch (NumberFormatException e) {
                        return List.of();
                    }
                });
        if (matched.isEmpty()) throw new ResourceNotFoundException("No SLA violation found for " + id + ".");
        matched.forEach(v -> v.dismiss(actor.getId()));
        return Map.of("success", true, "id", id, "dismissed", matched.size());
    }

    public int timeScale() { return timeScale; }
}
