package com.resolveit.service;

import com.resolveit.domain.audit.AuditEvent;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.event.IncidentEvent;
import com.resolveit.exception.ValidationException;
import com.resolveit.repository.AuditEventRepository;
import com.resolveit.web.dto.AuditEventDto;
import com.resolveit.web.mapper.IncidentMapper;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Append-only audit trail. Incident rows come from AuditObserver; admin actions use recordSystem. */
@Service
public class AuditService {
    private final AuditEventRepository repository;
    private final IncidentMapper mapper;
    private final Clock clock;
    private java.time.Instant lastRecorded;

    public AuditService(AuditEventRepository repository, IncidentMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Audit rows of one request share a timestamp; nudging by a microsecond keeps their order stable. */
    private synchronized java.time.Instant ordered(java.time.Instant at) {
        boolean sameMoment = lastRecorded != null && !at.isAfter(lastRecorded)
                && java.time.Duration.between(at, lastRecorded).toMillis() < 50;
        java.time.Instant next = sameMoment ? lastRecorded.plusNanos(1000) : at;
        lastRecorded = next;
        return next;
    }

    public void record(IncidentEvent event) {
        boolean moved = event.changedStatus();
        repository.save(new AuditEvent(ordered(event.occurredAt()), event.actorId(), event.incident().getPublicId(),
                event.type().name(), event.detail(), moved ? event.previousStatus().name() : null,
                moved ? event.incident().getStatus().name() : null));
    }

    /** Non-incident actions: account management, service catalog, settings, failed logins. */
    public void recordSystem(String actor, String action, String detail) {
        repository.save(new AuditEvent(ordered(clock.instant()), actor, AuditEvent.SYSTEM_SCOPE, action, detail, null, null));
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> page(PortalUser actor, int offset, int limit, String incident, String user, String action) {
        AccessGuard.requireAdmin(actor);
        if (offset < 0 || limit < 1 || limit > 500) throw new ValidationException("Invalid audit paging parameters.");
        boolean filtered = notBlank(incident) || notBlank(user) || notBlank(action);
        Stream<AuditEvent> events;
        if (!filtered && offset % limit == 0) {
            events = repository.findAllByOrderByOccurredAtDesc(PageRequest.of(offset / limit, limit)).stream();
        } else {
            events = repository.findAllByOrderByOccurredAtDesc().stream()
                    .filter(e -> !notBlank(incident) || contains(e.getIncidentId(), incident))
                    .filter(e -> !notBlank(user) || contains(e.getActor(), user))
                    .filter(e -> !notBlank(action) || contains(e.getAction(), action))
                    .skip(offset).limit(limit);
        }
        return events.map(mapper::audit).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> forIncidentAdmin(PortalUser actor, String incidentId) {
        AccessGuard.requireAdmin(actor);
        return forIncident(incidentId);
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> forIncident(String incidentId) {
        return repository.findByIncidentIdIgnoreCaseOrderByOccurredAtDesc(incidentId).stream().map(mapper::audit).toList();
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.strip().toLowerCase(Locale.ROOT));
    }
}
