package com.resolveit.service;

import com.resolveit.domain.catalog.ItService;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.NoteType;
import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventPublisher;
import com.resolveit.event.IncidentEventType;
import com.resolveit.exception.DuplicateIncidentException;
import com.resolveit.exception.InvalidStateTransitionException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.exception.TechnicianUnavailableException;
import com.resolveit.exception.UnauthorizedOperationException;
import com.resolveit.exception.ValidationException;
import com.resolveit.factory.IncidentFactory;
import com.resolveit.repository.FeedbackRepository;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.strategy.StrategyRegistry;
import com.resolveit.strategy.classify.CategoryClassifier;
import com.resolveit.strategy.classify.ClassificationResult;
import com.resolveit.strategy.priority.AffectedUsersAnalyzer;
import com.resolveit.strategy.priority.ImpactAnalyzer;
import com.resolveit.strategy.priority.PriorityInput;
import com.resolveit.strategy.priority.PriorityResult;
import com.resolveit.strategy.priority.PriorityStrategy;
import com.resolveit.strategy.priority.UrgencyAnalyzer;
import com.resolveit.util.TextNormalizer;
import com.resolveit.web.dto.AdminIncidentUpdate;
import com.resolveit.web.dto.CreateIncidentRequest;
import com.resolveit.web.dto.IncidentDto;
import com.resolveit.web.dto.RecommendationDto;
import com.resolveit.web.dto.SuggestionDto;
import com.resolveit.web.mapper.IncidentMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstraction: the single entry point of the incident pipeline. Composition: it is built from a classifier
 * registry, priority strategy registry, text analysers, SLA, assignment and correlation collaborators,
 * all injected through the constructor. Every step publishes an event, so audit and notifications are automatic.
 */
@Service
public class IncidentWorkflowService {
    /** Who may perform which lifecycle action (ownership is checked separately via PortalUser.canView). */
    private static final Map<Role, Set<String>> ALLOWED_ACTIONS = new EnumMap<>(Map.of(
            Role.EMPLOYEE, Set.of("approve", "reject-closure", "info"),
            Role.TECHNICIAN, Set.of("accept", "reject", "start", "note", "info", "request-info", "resolve", "approve", "reject-closure"),
            Role.ADMIN, Set.of()));
    private static final List<IncidentStatus> MAIN_PATH = List.of(IncidentStatus.NEW, IncidentStatus.CLASSIFIED,
            IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED);

    private final IncidentRepository incidents;
    private final FeedbackRepository feedback;
    private final IncidentFactory factory;
    private final StrategyRegistry<CategoryClassifier> classifiers;
    private final StrategyRegistry<PriorityStrategy> priorityStrategies;
    private final ImpactAnalyzer impactAnalyzer;
    private final UrgencyAnalyzer urgencyAnalyzer;
    private final AffectedUsersAnalyzer affectedUsersAnalyzer;
    private final CatalogService catalog;
    private final SettingsService settings;
    private final SlaService sla;
    private final AssignmentService assignment;
    private final IncidentCorrelationService correlation;
    private final IncidentEventPublisher events;
    private final IncidentMapper mapper;
    private final Clock clock;

    public IncidentWorkflowService(IncidentRepository incidents, FeedbackRepository feedback, IncidentFactory factory,
                                   StrategyRegistry<CategoryClassifier> classifiers,
                                   StrategyRegistry<PriorityStrategy> priorityStrategies, ImpactAnalyzer impactAnalyzer,
                                   UrgencyAnalyzer urgencyAnalyzer, AffectedUsersAnalyzer affectedUsersAnalyzer,
                                   CatalogService catalog, SettingsService settings, SlaService sla,
                                   AssignmentService assignment, IncidentCorrelationService correlation,
                                   IncidentEventPublisher events, IncidentMapper mapper, Clock clock) {
        this.incidents = incidents;
        this.feedback = feedback;
        this.factory = factory;
        this.classifiers = classifiers;
        this.priorityStrategies = priorityStrategies;
        this.impactAnalyzer = impactAnalyzer;
        this.urgencyAnalyzer = urgencyAnalyzer;
        this.affectedUsersAnalyzer = affectedUsersAnalyzer;
        this.catalog = catalog;
        this.settings = settings;
        this.sla = sla;
        this.assignment = assignment;
        this.correlation = correlation;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ creation pipeline

    @Transactional
    public IncidentDto create(PortalUser reporter, CreateIncidentRequest request) {
        return mapper.toDto(createIncident(reporter, request));
    }

    /** The 10-step pipeline: validate, duplicate check, create, classify, service, assess, prioritise, SLA, correlate, assign. */
    @Transactional
    public Incident createIncident(PortalUser reporter, CreateIncidentRequest request) {
        AccessGuard.requireRole(reporter, Role.EMPLOYEE);
        Instant at = clock.instant();
        // 1. validate and strip HTML
        String title = TextNormalizer.plain(request.title());
        String description = TextNormalizer.plain(request.description());
        // 2. duplicate check (same reporter, open, similarity >= 0.85)
        var duplicate = correlation.findDuplicate(reporter.getId(), title, description);
        boolean force = Boolean.TRUE.equals(request.force());
        if (duplicate.isPresent() && !force) throw new DuplicateIncidentException(duplicate.get().getPublicId());
        // 3. create in status NEW
        int requestedReach = request.affectedUsers() == null ? 0 : request.affectedUsers();
        String text = title + " " + description;
        int affected = requestedReach > 0 ? requestedReach : affectedUsersAnalyzer.analyze(text);
        Incident incident = incidents.save(factory.create(reporter, title, description, affected, at));
        events.publish(IncidentEvent.of(IncidentEventType.INCIDENT_CREATED, incident, reporter.getId(),
                incident.getPublicId() + " created by " + reporter.getRole().getLabel() + " " + reporter.getId() + ".", null, at));
        // 4-5. classify and identify the affected service
        ClassificationResult classification = classifiers.get(settings.classifierStrategy())
                .classify(title, description, catalog.activeCategories());
        ItService service = catalog.resolveService(request.service(), text);
        incident.classify(classification.category(), classification.confidence(), service.getName(), service.getCriticality(), at);
        events.publish(IncidentEvent.of(IncidentEventType.CLASSIFIED, incident, IncidentEvent.SYSTEM,
                "Incident classified as " + classification.category().toUpperCase(Locale.ROOT) + " (confidence "
                        + classification.confidence() + "%); affected service " + service.getName() + ".", IncidentStatus.NEW, at));
        // 6-8. impact, urgency, priority and SLA deadlines
        assess(incident, impactAnalyzer.analyze(text), urgencyAnalyzer.analyze(text), service.getCriticality(), affected);
        sla.applyDeadlines(incident, at);
        events.publish(IncidentEvent.of(IncidentEventType.PRIORITY_CALCULATED, incident, IncidentEvent.SYSTEM,
                priorityDetail(incident) + " SLA started: respond by " + incident.getResponseDueAt() + ", resolve by "
                        + incident.getResolutionDueAt() + ".", null, at));
        if (duplicate.isPresent()) {
            incident.markDuplicateOf(duplicate.get().getPublicId());
            events.publish(IncidentEvent.of(IncidentEventType.DUPLICATE_OVERRIDDEN, incident, reporter.getId(),
                    "Submitted despite a similar open incident " + duplicate.get().getPublicId() + ".", null, at));
        }
        // 9. related incidents from other reporters
        correlation.correlate(incident, at);
        // 10. assignment through the active strategy
        assignment.tryAssign(incident, Set.of(), true, at);
        return incident;
    }

    private void assess(Incident incident, int impact, int urgency, int criticality, int affected) {
        incident.applyAssessment(impact, urgency, criticality, affected);
        PriorityStrategy strategy = priorityStrategies.get(settings.priorityStrategy());
        PriorityResult result = strategy.compute(new PriorityInput(impact, urgency, criticality, affected));
        incident.applyPriority(result.priority(), result.score(), result.strategy());
    }

    private static String priorityDetail(Incident i) {
        return "Priority calculated as " + i.getPriority() + " (score " + i.getPriorityScore() + ", " + i.getPriorityStrategy()
                + "; impact " + i.getImpact() + ", urgency " + i.getUrgency() + ", criticality " + i.getCriticality()
                + ", affected users " + i.getAffectedUsers() + ").";
    }

    // ------------------------------------------------------------------ lifecycle actions

    @Transactional
    public IncidentDto action(PortalUser actor, String id, String action, Map<String, String> body) {
        Incident incident = find(id);
        perform(actor, incident, action == null ? "" : action.toLowerCase(Locale.ROOT), body == null ? Map.of() : body);
        return mapper.toDto(incident);
    }

    /** Executes one lifecycle action. Legality of the move itself is decided by the incident's State object. */
    public void perform(PortalUser actor, Incident incident, String action, Map<String, String> body) {
        AccessGuard.requireView(actor, incident);
        if (!ALLOWED_ACTIONS.get(actor.getRole()).contains(action)) {
            if (ALLOWED_ACTIONS.values().stream().noneMatch(s -> s.contains(action))) throw new ValidationException("Unknown incident action.");
            throw new UnauthorizedOperationException("Your role cannot perform this action.");
        }
        Instant at = clock.instant();
        IncidentStatus before = incident.getStatus();
        String content = body.getOrDefault("content", "");
        switch (action) {
            case "accept" -> assignment.accept(actor, incident, at);
            case "reject" -> {
                if (incident.getStatus() != IncidentStatus.ASSIGNED) throw new InvalidStateTransitionException(before.name(), "reject", "This offer is no longer available.");
                assignment.decline(actor, incident, body.get("reason"), at);
            }
            case "start" -> {
                incident.startWork(at);
                publish(IncidentEventType.WORK_STARTED, incident, actor, "Investigation started.", before, at);
            }
            case "note" -> {
                incident.addNote(actor, NoteType.INVESTIGATION, content, at);
                publish(IncidentEventType.NOTE_ADDED, incident, actor, content.strip(), null, at);
            }
            case "info" -> {
                boolean employee = actor.hasRole(Role.EMPLOYEE);
                incident.addNote(actor, employee ? NoteType.INFO_FROM_EMPLOYEE : NoteType.INVESTIGATION, content, at);
                publish(employee ? IncidentEventType.INFO_PROVIDED : IncidentEventType.NOTE_ADDED, incident, actor, content.strip(), null, at);
            }
            case "request-info" -> {
                String question = content.isBlank() ? "Could you share a little more detail about when this started?" : content;
                incident.addNote(actor, NoteType.INFO_REQUEST, question, at);
                incident.addChatMessage(actor, "Information requested: " + question.strip(), at);
                publish(IncidentEventType.INFO_REQUESTED, incident, actor, question.strip(), null, at);
            }
            case "resolve" -> {
                incident.resolve(body.get("summary"), body.get("rootCause"), body.get("steps"), at);
                incident.addNote(actor, NoteType.RESOLUTION, incident.getResolutionSummary(), at);
                publish(IncidentEventType.INCIDENT_RESOLVED, incident, actor, "Technician submitted a resolution: "
                        + incident.getResolutionSummary(), before, at);
                sla.evaluate(incident, at);
            }
            case "approve" -> {
                boolean technicianSide = actor.hasRole(Role.TECHNICIAN);
                boolean closed = incident.approveClosure(technicianSide, at);
                String other = technicianSide ? incident.getReporterId() : incident.getTechnicianId();
                publish(IncidentEventType.CLOSURE_APPROVED, incident, actor, actor.getRole().getLabel() + " approved the resolution."
                        + (closed ? "" : " Waiting for the other party."), null, at, other);
                if (closed) {
                    publish(IncidentEventType.INCIDENT_CLOSED, incident, actor, "Both sides approved the resolution; incident verified and closed.", before, at);
                }
            }
            case "reject-closure" -> {
                incident.rejectClosure(at);
                String other = actor.hasRole(Role.TECHNICIAN) ? incident.getReporterId() : incident.getTechnicianId();
                String reason = body.getOrDefault("reason", "").strip();
                publish(IncidentEventType.CLOSURE_REJECTED, incident, actor, "Resolution was rejected; work resumed."
                        + (reason.isEmpty() ? "" : " Reason: " + reason), before, at, other);
            }
            default -> throw new ValidationException("Unknown incident action.");
        }
    }

    private void publish(IncidentEventType type, Incident incident, PortalUser actor, String detail, IncidentStatus before, Instant at) {
        publish(type, incident, actor, detail, before, at, null);
    }

    private void publish(IncidentEventType type, Incident incident, PortalUser actor, String detail, IncidentStatus before,
                         Instant at, String recipient) {
        events.publish(IncidentEvent.of(type, incident, actor.getId(), detail, before, at)
                .with("recipient", recipient).with("actorName", actor.getName()));
    }

    // ------------------------------------------------------------------ feedback

    @Transactional
    public IncidentDto leaveFeedback(PortalUser employee, String id, int rating, String comment) {
        Incident incident = find(id);
        AccessGuard.requireView(employee, incident);
        Instant at = clock.instant();
        var saved = feedback.save(incident.leaveFeedback(employee, rating, comment, feedback.existsByIncident(incident), at));
        events.publish(IncidentEvent.of(IncidentEventType.FEEDBACK_SUBMITTED, incident, employee.getId(),
                saved.getRating() + "/5" + (saved.getComment().isEmpty() ? "" : " - " + saved.getComment()), null, at));
        return mapper.toDto(incident, saved);
    }

    // ------------------------------------------------------------------ admin controls

    /** Edits text, category, impact, urgency or reach; priority and SLA are always recomputed together. */
    @Transactional
    public IncidentDto adminEdit(PortalUser admin, String id, AdminIncidentUpdate update) {
        AccessGuard.requireAdmin(admin);
        Incident incident = find(id);
        if (!incident.isOpen()) throw new InvalidStateTransitionException(incident.getStatus().name(), "edit", "Closed incidents cannot be edited.");
        Instant at = clock.instant();
        incident.updateDetails(update.title() == null ? incident.getTitle() : update.title(),
                update.description() == null ? incident.getDescription() : update.description());
        if (update.category() != null && !update.category().isBlank()) {
            if (catalog.validCategoryNames().stream().noneMatch(c -> c.equalsIgnoreCase(update.category().strip()))) {
                throw ValidationException.field("category", "Choose an active category.");
            }
            incident.changeCategory(update.category());
        }
        Priority before = incident.getPriority();
        assess(incident, value(update.impact(), incident.getImpact()), value(update.urgency(), incident.getUrgency()),
                incident.getCriticality(), value(update.affectedUsers(), incident.getAffectedUsers()));
        sla.applyDeadlines(incident, at);
        events.publish(IncidentEvent.of(IncidentEventType.INCIDENT_EDITED, incident, admin.getId(),
                "Admin updated incident details and recalculated priority. " + priorityDetail(incident), null, at));
        if (before != incident.getPriority()) {
            events.publish(IncidentEvent.of(IncidentEventType.PRIORITY_CHANGED, incident, admin.getId(),
                    "Priority changed from " + before + " to " + incident.getPriority() + "; SLA deadlines recalculated.", null, at));
        }
        return mapper.toDto(incident);
    }

    /** Admin lifecycle override: at most two steps forward on the main path, never to VERIFIED or CLOSED. */
    @Transactional
    public IncidentDto adminAdvance(PortalUser admin, String id, String target) {
        AccessGuard.requireAdmin(admin);
        Incident incident = find(id);
        IncidentStatus goal;
        try {
            goal = IncidentStatus.valueOf(target.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ValidationException.field("status", "Unknown status.");
        }
        int from = MAIN_PATH.indexOf(incident.getStatus());
        int to = MAIN_PATH.indexOf(goal);
        if (goal == IncidentStatus.VERIFIED || goal == IncidentStatus.CLOSED || from < 0 || to <= from || to - from > 2) {
            throw new InvalidStateTransitionException(incident.getStatus().name(), "advance",
                    "Admins may move an incident forward by one or two steps, never to VERIFIED or CLOSED.");
        }
        Instant at = clock.instant();
        IncidentStatus before = incident.getStatus();
        for (int step = from + 1; step <= to; step++) {
            IncidentStatus next = MAIN_PATH.get(step);
            if (next == IncidentStatus.ASSIGNED) {
                if (!assignment.tryAssign(incident, Set.of(), false, at)) throw new TechnicianUnavailableException();
            } else {
                incident.transitionTo(next, at);
            }
        }
        events.publish(IncidentEvent.of(IncidentEventType.STATUS_OVERRIDDEN, incident, admin.getId(),
                "Administrator moved the incident from " + before + " to " + incident.getStatus() + ".", before, at));
        return mapper.toDto(incident);
    }

    @Transactional
    public IncidentDto reassign(PortalUser admin, String id, String technicianId) {
        Incident incident = find(id);
        assignment.manualReassign(admin, incident, technicianId, clock.instant());
        return mapper.toDto(incident);
    }

    @Transactional
    public IncidentDto unassign(PortalUser admin, String id) {
        Incident incident = find(id);
        assignment.unassign(admin, incident, clock.instant());
        return mapper.toDto(incident);
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> recommendations(PortalUser admin, String id) {
        AccessGuard.requireAdmin(admin);
        return assignment.recommend(find(id));
    }

    @Transactional(readOnly = true)
    public List<SuggestionDto> suggestions(PortalUser actor, String id) {
        AccessGuard.requireRole(actor, Role.TECHNICIAN, Role.ADMIN);
        Incident incident = find(id);
        AccessGuard.requireView(actor, incident);
        return correlation.suggestResolutions(incident);
    }

    public Incident find(String id) {
        String key = id == null ? "" : id.strip();
        return incidents.findByPublicIdIgnoreCase(key).or(() -> {
            try {
                return incidents.findById(Long.parseLong(key));
            } catch (NumberFormatException e) {
                return java.util.Optional.empty();
            }
        }).orElseThrow(() -> new ResourceNotFoundException("Incident not found."));
    }

    private static int value(Integer requested, int current) { return requested == null ? current : requested; }
}
