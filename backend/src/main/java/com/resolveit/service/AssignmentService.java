package com.resolveit.service;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Technician;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventPublisher;
import com.resolveit.event.IncidentEventType;
import com.resolveit.exception.InvalidStateTransitionException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.exception.TechnicianUnavailableException;
import com.resolveit.exception.ValidationException;
import com.resolveit.exception.WorkloadExceededException;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.repository.TechnicianRepository;
import com.resolveit.strategy.StrategyRegistry;
import com.resolveit.strategy.assignment.AssignmentStrategy;
import com.resolveit.strategy.assignment.TechnicianCandidate;
import com.resolveit.web.dto.RecommendationDto;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Technician assignment: eligibility filter, runtime-selected AssignmentStrategy, offers, declines,
 * manual reassignment, retry of unassigned incidents (P1 first) and smart recommendations.
 */
@Service
public class AssignmentService {
    private final TechnicianRepository technicians;
    private final IncidentRepository incidents;
    private final CatalogService catalog;
    private final SettingsService settings;
    private final StrategyRegistry<AssignmentStrategy> strategies;
    private final IncidentEventPublisher events;
    private final Clock clock;

    public AssignmentService(TechnicianRepository technicians, IncidentRepository incidents, CatalogService catalog,
                             SettingsService settings, StrategyRegistry<AssignmentStrategy> strategies,
                             IncidentEventPublisher events, Clock clock) {
        this.technicians = technicians;
        this.incidents = incidents;
        this.catalog = catalog;
        this.settings = settings;
        this.strategies = strategies;
        this.events = events;
        this.clock = clock;
    }

    public long workload(String technicianId) {
        return incidents.countByTechnicianIdAndStatusIn(technicianId, IncidentStatus.WORKLOAD);
    }

    public Map<String, Long> workloads() {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : incidents.countWorkloadByTechnician(IncidentStatus.WORKLOAD)) result.put((String) row[0], (Long) row[1]);
        return result;
    }

    /** Active, linked to the incident's service, has capacity, never declined it, not explicitly excluded. */
    public List<TechnicianCandidate> eligible(Incident incident, Set<String> excluded) {
        Map<String, Long> loads = workloads();
        return technicians.findByActiveTrueOrderByIdAsc().stream()
                .filter(t -> !excluded.contains(t.getId()) && !incident.getDeclinedBy().contains(t.getId()))
                .filter(t -> catalog.canServe(t, incident.getServiceName()))
                .map(t -> new TechnicianCandidate(t, loads.getOrDefault(t.getId(), 0L),
                        incidents.countByTechnicianIdAndServiceNameIgnoreCase(t.getId(), incident.getServiceName())))
                .filter(c -> c.technician().hasCapacity(c.load()))
                .toList();
    }

    /** Strategy pattern in action: the active strategy is looked up by the admin's setting on every call. */
    public Technician selectTechnician(Incident incident, Set<String> excluded) {
        List<TechnicianCandidate> candidates = eligible(incident, excluded);
        AssignmentStrategy strategy = strategies.get(settings.assignmentStrategy());
        return strategy.select(incident, candidates).map(TechnicianCandidate::technician)
                .orElseThrow(() -> new TechnicianUnavailableException(
                        "No active technician linked to " + incident.getServiceName() + " has capacity."));
    }

    /** Offers the incident to the best technician. Returns false (and optionally alerts admins) when nobody is eligible. */
    public boolean tryAssign(Incident incident, Set<String> excluded, boolean announceFailure, Instant at) {
        if (incident.getStatus() != IncidentStatus.CLASSIFIED) return false;
        try {
            Technician chosen = selectTechnician(incident, excluded);
            incident.offerTo(chosen, at);
            events.publish(IncidentEvent.of(IncidentEventType.OFFER_CREATED, incident, IncidentEvent.SYSTEM,
                    "Offered to " + chosen.getName() + " (" + chosen.getId() + ") using the "
                            + strategies.get(settings.assignmentStrategy()).displayName().toLowerCase() + " strategy.",
                    IncidentStatus.CLASSIFIED, at));
            return true;
        } catch (TechnicianUnavailableException none) {
            if (announceFailure) {
                events.publish(IncidentEvent.of(IncidentEventType.INCIDENT_UNASSIGNABLE, incident, IncidentEvent.SYSTEM,
                        none.getMessage() + " It will be retried automatically.", null, at));
            }
            return false;
        }
    }

    public void accept(PortalUser technician, Incident incident, Instant at) {
        IncidentStatus before = incident.getStatus();
        incident.acknowledge(at);
        events.publish(IncidentEvent.of(IncidentEventType.OFFER_ACCEPTED, incident, technician.getId(),
                "Technician " + technician.getName() + " accepted the assignment.", before, at));
    }

    public void decline(PortalUser technician, Incident incident, String reason, Instant at) {
        IncidentStatus before = incident.getStatus();
        String previous = incident.decline(at);
        String why = reason == null || reason.isBlank() ? "" : " Reason: " + reason.strip();
        events.publish(IncidentEvent.of(IncidentEventType.OFFER_REJECTED, incident, technician.getId(),
                "Technician " + technician.getName() + " passed on the assignment." + why, before, at));
        tryAssign(incident, previous == null ? Set.of() : Set.of(previous), true, at);
    }

    /** Admin chooses a specific technician; capacity and service qualification are still enforced. */
    public void manualReassign(PortalUser admin, Incident incident, String technicianId, Instant at) {
        AccessGuard.requireAdmin(admin);
        if (!incident.isOpen() || incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.VERIFIED) {
            throw new InvalidStateTransitionException(incident.getStatus().name(), "reassign", "Only open incidents can be reassigned.");
        }
        Technician technician = technicians.findById(technicianId == null ? "" : technicianId.strip().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found."));
        if (!technician.isActive() || !catalog.canServe(technician, incident.getServiceName())) {
            throw new ValidationException("Technician must be active and qualified for this service.");
        }
        long load = workload(technician.getId());
        if (technician.getId().equals(incident.getTechnicianId())) load--;
        if (!technician.hasCapacity(load)) throw new WorkloadExceededException(technician.getName() + " has reached the maximum workload.");
        IncidentStatus before = incident.getStatus();
        if (before != IncidentStatus.CLASSIFIED) incident.release(at);
        incident.offerTo(technician, at);
        events.publish(IncidentEvent.of(IncidentEventType.INCIDENT_REASSIGNED, incident, admin.getId(),
                "Admin reassigned the incident to " + technician.getName() + ".", before, at));
    }

    /** Admin removes the technician; the system immediately re-runs assignment without them. */
    public void unassign(PortalUser admin, Incident incident, Instant at) {
        AccessGuard.requireAdmin(admin);
        IncidentStatus before = incident.getStatus();
        String previous = incident.release(at);
        events.publish(IncidentEvent.of(IncidentEventType.TECHNICIAN_REMOVED, incident, admin.getId(),
                "Administrator removed the technician; the incident returns to the queue.", before, at)
                .with("previousTechnician", previous));
        tryAssign(incident, previous == null ? Set.of() : Set.of(previous), true, at);
    }

    /** A deactivated technician's open work is released and re-offered to colleagues. */
    public int releaseAllFor(PortalUser admin, Technician technician, Instant at) {
        int released = 0;
        for (Incident incident : incidents.findByTechnicianIdOrderByCreatedAtDesc(technician.getId())) {
            IncidentStatus s = incident.getStatus();
            if (s != IncidentStatus.ASSIGNED && s != IncidentStatus.ACKNOWLEDGED && s != IncidentStatus.IN_PROGRESS) continue;
            incident.release(at);
            events.publish(IncidentEvent.of(IncidentEventType.TECHNICIAN_REMOVED, incident, admin.getId(),
                    "Assigned technician " + technician.getName() + " was deactivated; incident returned to the queue.", s, at));
            tryAssign(incident, Set.of(technician.getId()), true, at);
            released++;
        }
        return released;
    }

    /** Retry job: unassigned incidents ordered in a PriorityQueue, P1 first, then oldest first. */
    @Transactional
    public int retryUnassigned() {
        Instant now = clock.instant();
        PriorityQueue<Incident> queue = new PriorityQueue<>(Comparator
                .comparing((Incident i) -> i.getPriority() == null ? Priority.P4 : i.getPriority())
                .thenComparing(Incident::getCreatedAt));
        queue.addAll(incidents.findByStatusInOrderByCreatedAtAsc(Set.of(IncidentStatus.CLASSIFIED)));
        int assigned = 0;
        while (!queue.isEmpty()) {
            if (tryAssign(queue.poll(), Set.of(), false, now)) assigned++;
        }
        return assigned;
    }

    /**
     * Smart technician recommendation for manual reassignment:
     * score = 0.4 x skill + 0.3 x (1 - load/max) + 0.2 x experience/maxExperience + 0.1 x pastOnService/maxPast.
     */
    public List<RecommendationDto> recommend(Incident incident) {
        List<Technician> all = technicians.findByActiveTrueOrderByIdAsc();
        Map<String, Long> loads = workloads();
        int maxExperience = all.stream().mapToInt(Technician::getExperienceYears).max().orElse(1);
        Map<String, Long> past = new HashMap<>();
        all.forEach(t -> past.put(t.getId(), incidents.countByTechnicianIdAndServiceNameIgnoreCase(t.getId(), incident.getServiceName())));
        long maxPast = Math.max(1, past.values().stream().mapToLong(Long::longValue).max().orElse(1));
        return all.stream().map(t -> {
            long load = loads.getOrDefault(t.getId(), 0L);
            boolean linked = catalog.canServe(t, incident.getServiceName());
            double skill = t.canHandle(incident.getCategory()) ? 1.0 : linked ? 0.5 : 0.0;
            double loadScore = Math.max(0, 1.0 - (double) load / Math.max(1, t.getMaxWorkload()));
            double experience = (double) t.getExperienceYears() / Math.max(1, maxExperience);
            double history = (double) past.get(t.getId()) / maxPast;
            double score = 0.4 * skill + 0.3 * loadScore + 0.2 * experience + 0.1 * history;
            boolean eligible = linked && t.hasCapacity(load) && !incident.getDeclinedBy().contains(t.getId());
            String note = !linked ? "Not linked to " + incident.getServiceName() : !t.hasCapacity(load) ? "At maximum workload"
                    : incident.getDeclinedBy().contains(t.getId()) ? "Declined this incident" : "Eligible";
            return new RecommendationDto(t.getId(), t.getName(), round(score), skill, round(loadScore), round(experience),
                    round(history), load, t.getMaxWorkload(), t.getExperienceYears(), past.get(t.getId()), eligible, note);
        }).sorted(Comparator.comparing(RecommendationDto::eligible).reversed()
                .thenComparing(Comparator.comparingDouble(RecommendationDto::score).reversed())).toList();
    }

    private static double round(double value) { return Math.round(value * 1000) / 1000.0; }
}
