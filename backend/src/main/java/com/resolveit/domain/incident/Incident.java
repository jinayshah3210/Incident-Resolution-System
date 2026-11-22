package com.resolveit.domain.incident;

import com.resolveit.domain.incident.state.IncidentState;
import com.resolveit.domain.sla.SlaClockState;
import com.resolveit.domain.sla.SlaStatus;
import com.resolveit.domain.sla.ViolationType;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Technician;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.InvalidStateTransitionException;
import com.resolveit.exception.UnauthorizedOperationException;
import com.resolveit.exception.ValidationException;
import com.resolveit.factory.IncidentStateFactory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregate root of the incident lifecycle.
 * Encapsulation: there is no public status setter; status changes only through behaviour methods,
 * each of which delegates the legality check to the current {@link IncidentState} (State pattern).
 * Composition: notes and chat messages are owned by the incident and cascade with it.
 */
@Entity
@Table(name = "incidents")
public class Incident {
    public static final int TITLE_MIN = 5;
    public static final int TITLE_MAX = 120;
    public static final int DESCRIPTION_MIN = 10;
    public static final int DESCRIPTION_MAX = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 20)
    private String publicId;
    @Column(name = "sequence_number", nullable = false, unique = true)
    private long sequenceNumber;
    @Column(nullable = false, length = TITLE_MAX)
    private String title;
    @Column(nullable = false, length = DESCRIPTION_MAX)
    private String description;
    @Column(name = "reporter_id", nullable = false, length = 20)
    private String reporterId;
    @Column(name = "reporter_name", length = 120)
    private String reporterName;
    @Column(name = "technician_id", length = 20)
    private String technicianId;
    @Column(name = "technician_name", length = 120)
    private String technicianName;
    @Column(length = 60)
    private String category;
    @Column(name = "classification_confidence", nullable = false)
    private int classificationConfidence;
    @Column(name = "service_name", length = 120)
    private String serviceName;
    @Column(nullable = false)
    private int impact = 1;
    @Column(nullable = false)
    private int urgency = 1;
    @Column(nullable = false)
    private int criticality = 1;
    @Column(name = "affected_users", nullable = false)
    private int affectedUsers = 1;
    @Column(name = "priority_score", nullable = false)
    private int priorityScore;
    @Enumerated(EnumType.STRING)
    @Column(length = 4)
    private Priority priority;
    @Column(name = "priority_strategy", length = 30)
    private String priorityStrategy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "classified_at")
    private Instant classifiedAt;
    @Column(name = "assigned_at")
    private Instant assignedAt;
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Column(name = "closed_at")
    private Instant closedAt;
    @Column(name = "response_due_at")
    private Instant responseDueAt;
    @Column(name = "resolution_due_at")
    private Instant resolutionDueAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "response_sla", nullable = false, length = 12)
    private SlaClockState responseSla = SlaClockState.ON_TRACK;
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_sla", nullable = false, length = 12)
    private SlaClockState resolutionSla = SlaClockState.ON_TRACK;
    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status", nullable = false, length = 12)
    private SlaStatus slaStatus = SlaStatus.OK;
    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel;
    @Column(name = "resolution_summary", length = 2000)
    private String resolutionSummary;
    @Column(name = "root_cause", length = 2000)
    private String rootCause;
    @Column(name = "resolution_steps", length = 4000)
    private String resolutionSteps;
    @Column(name = "technician_approved", nullable = false)
    private boolean technicianApproved;
    @Column(name = "employee_approved", nullable = false)
    private boolean employeeApproved;
    @Column(name = "correlation_group", length = 20)
    private String correlationGroup;
    @Column(name = "duplicate_of", length = 20)
    private String duplicateOf;
    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<IncidentNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC, id ASC")
    private List<ChatMessage> chatMessages = new ArrayList<>();

    /** Technicians who declined this incident; they are never offered it again. */
    @ElementCollection
    @CollectionTable(name = "incident_declines", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "technician_id", length = 20)
    private Set<String> declinedBy = new HashSet<>();

    protected Incident() { }

    /** Created only through IncidentFactory, always in status NEW. */
    public Incident(long sequenceNumber, String title, String description, PortalUser reporter, int affectedUsers, Instant createdAt) {
        validateText(title, description);
        this.sequenceNumber = sequenceNumber;
        this.publicId = "INC" + sequenceNumber;
        this.title = title.strip();
        this.description = description.strip();
        this.reporterId = reporter.getId();
        this.reporterName = reporter.getName();
        this.affectedUsers = requireScale(affectedUsers, "affectedUsers");
        this.status = IncidentStatus.NEW;
        this.createdAt = createdAt;
    }

    // ------------------------------------------------------------------ State pattern delegation

    private IncidentState state() { return IncidentStateFactory.forStatus(status); }

    private void moveTo(IncidentStatus next, Instant at) {
        status = next;
        switch (next) {
            case CLASSIFIED -> { if (classifiedAt == null) classifiedAt = at; }
            case ASSIGNED -> assignedAt = at;
            case ACKNOWLEDGED -> acknowledgedAt = at;
            case IN_PROGRESS -> { if (startedAt == null) startedAt = at; }
            case RESOLVED -> resolvedAt = at;
            case VERIFIED -> verifiedAt = at;
            case CLOSED -> closedAt = at;
            default -> { }
        }
        recomputeSlaStatus();
    }

    /** NEW -> CLASSIFIED with the classifier's category and the affected service. */
    public void classify(String category, int confidence, String serviceName, int serviceCriticality, Instant at) {
        IncidentStatus next = state().classify();
        this.category = category;
        this.classificationConfidence = Math.max(0, Math.min(100, confidence));
        this.serviceName = serviceName;
        this.criticality = requireScale(serviceCriticality, "criticality");
        moveTo(next, at);
    }

    /** CLASSIFIED -> ASSIGNED: the system offers the incident to one technician. */
    public void offerTo(Technician technician, Instant at) {
        if (declinedBy.contains(technician.getId())) {
            throw new InvalidStateTransitionException(status.name(), "assign",
                    "This technician already declined " + publicId + ".");
        }
        IncidentStatus next = state().offer();
        this.technicianId = technician.getId();
        this.technicianName = technician.getName();
        moveTo(next, at);
    }

    /** ASSIGNED -> ACKNOWLEDGED: the technician accepts; the response clock is met if on time. */
    public void acknowledge(Instant at) {
        IncidentStatus next = state().acknowledge();
        moveTo(next, at);
        meetClock(ViolationType.RESPONSE, at, responseDueAt);
    }

    /** ASSIGNED -> CLASSIFIED: the technician declines; they are excluded from future offers. */
    public String decline(Instant at) {
        String previous = technicianId;
        release(at);
        if (previous != null) declinedBy.add(previous);
        return previous;
    }

    /** ASSIGNED / ACKNOWLEDGED / IN_PROGRESS -> CLASSIFIED: technician removed (admin or deactivation). */
    public String release(Instant at) {
        IncidentStatus next = state().release();
        String previous = technicianId;
        technicianId = null;
        technicianName = null;
        assignedAt = null;
        resetApprovals();
        moveTo(next, at);
        return previous;
    }

    /** ACKNOWLEDGED -> IN_PROGRESS. */
    public void startWork(Instant at) { moveTo(state().start(), at); }

    /** IN_PROGRESS -> RESOLVED. Resolution details are mandatory. */
    public void resolve(String summary, String root, String steps, Instant at) {
        String cleanSummary = summary == null ? "" : summary.strip();
        if (cleanSummary.length() < 5) throw ValidationException.field("summary", "Resolution summary is required.");
        IncidentStatus next = state().resolve();
        this.resolutionSummary = cleanSummary;
        this.rootCause = root == null ? "" : root.strip();
        this.resolutionSteps = steps == null ? "" : steps.strip();
        resetApprovals();
        moveTo(next, at);
        meetClock(ViolationType.RESOLUTION, at, resolutionDueAt);
    }

    /**
     * Closure handshake. Records one side's approval; when both sides approved the incident
     * moves RESOLVED -> VERIFIED -> CLOSED automatically. Returns true when it closed.
     */
    public boolean approveClosure(boolean technicianSide, Instant at) {
        if (!state().acceptsClosureReview()) {
            throw new InvalidStateTransitionException(status.name(), "approve closure",
                    "Closure can only be reviewed after the incident is resolved.");
        }
        if (technicianSide) technicianApproved = true; else employeeApproved = true;
        if (!(technicianApproved && employeeApproved)) return false;
        moveTo(state().verify(), at);
        moveTo(state().close(), at);
        return true;
    }

    /** RESOLVED -> IN_PROGRESS when either party rejects the resolution. */
    public void rejectClosure(Instant at) {
        if (!state().acceptsClosureReview()) {
            throw new InvalidStateTransitionException(status.name(), "reject closure",
                    "Closure can only be reviewed after the incident is resolved.");
        }
        IncidentStatus next = state().reopen();
        resetApprovals();
        resolvedAt = null;
        if (resolutionSla == SlaClockState.MET) resolutionSla = SlaClockState.ON_TRACK;
        moveTo(next, at);
    }

    /**
     * Generic move used by the admin "advance" control and by tests. It maps the target to the
     * matching state action, so illegal targets are still rejected by the State objects.
     */
    public void transitionTo(IncidentStatus target, Instant at) {
        switch (target) {
            case NEW -> throw state().illegal("move back to new");
            case CLASSIFIED -> {
                if (status == IncidentStatus.NEW) moveTo(state().classify(), at);
                else release(at);
            }
            case ASSIGNED -> {
                if (technicianId == null) {
                    IncidentStatus next = state().offer();
                    throw new InvalidStateTransitionException(status.name(), "assign",
                            "A technician must be chosen before the incident can move to " + next + ".");
                }
                moveTo(state().offer(), at);
            }
            case ACKNOWLEDGED -> acknowledge(at);
            case IN_PROGRESS -> moveTo(status == IncidentStatus.RESOLVED ? state().reopen() : state().start(), at);
            case RESOLVED -> {
                if (resolutionSummary == null || resolutionSummary.isBlank()) {
                    state().resolve();
                    throw ValidationException.field("summary", "Resolution details are required before resolving.");
                }
                moveTo(state().resolve(), at);
                meetClock(ViolationType.RESOLUTION, at, resolutionDueAt);
            }
            case VERIFIED -> moveTo(state().verify(), at);
            case CLOSED -> moveTo(state().close(), at);
        }
    }

    // ------------------------------------------------------------------ assessment and priority

    public void applyAssessment(int impact, int urgency, int criticality, int affectedUsers) {
        this.impact = requireScale(impact, "impact");
        this.urgency = requireScale(urgency, "urgency");
        this.criticality = requireScale(criticality, "criticality");
        this.affectedUsers = requireScale(affectedUsers, "affectedUsers");
    }

    public void applyPriority(Priority newPriority, int score, String strategyKey) {
        this.priority = newPriority;
        this.priorityScore = score;
        this.priorityStrategy = strategyKey;
    }

    /** Raises priority by one band (used for suspected major incidents). Returns false if already P1. */
    public boolean raisePriority() {
        if (priority == null || priority == Priority.P1) return false;
        priority = priority.higher();
        return true;
    }

    public void updateDetails(String newTitle, String newDescription) {
        validateText(newTitle, newDescription);
        this.title = newTitle.strip();
        this.description = newDescription.strip();
    }

    public void changeCategory(String newCategory) {
        if (newCategory == null || newCategory.isBlank()) throw ValidationException.field("category", "Choose a category.");
        this.category = newCategory.strip();
    }

    // ------------------------------------------------------------------ SLA clocks

    /** Sets both deadlines (recomputed from createdAt) and resets warning flags of clocks still running. */
    public void applySlaDeadlines(Instant responseDue, Instant resolutionDue) {
        this.responseDueAt = responseDue;
        this.resolutionDueAt = resolutionDue;
        if (responseSla == SlaClockState.APPROACHING) responseSla = SlaClockState.ON_TRACK;
        if (resolutionSla == SlaClockState.APPROACHING) resolutionSla = SlaClockState.ON_TRACK;
        recomputeSlaStatus();
    }

    public SlaClockState clock(ViolationType type) {
        return type == ViolationType.RESPONSE ? responseSla : resolutionSla;
    }

    public Instant dueAt(ViolationType type) {
        return type == ViolationType.RESPONSE ? responseDueAt : resolutionDueAt;
    }

    /** Whether the clock is still running (not yet met). */
    public boolean isClockRunning(ViolationType type) {
        if (type == ViolationType.RESPONSE) return !status.hasResponded();
        return resolvedAt == null && status != IncidentStatus.VERIFIED && status != IncidentStatus.CLOSED;
    }

    /** ON_TRACK -> APPROACHING exactly once. Returns true when the state changed. */
    public boolean markApproaching(ViolationType type) {
        if (clock(type) != SlaClockState.ON_TRACK) return false;
        setClock(type, SlaClockState.APPROACHING);
        return true;
    }

    /** Any running clock -> VIOLATED exactly once. Returns true when the state changed. */
    public boolean markViolated(ViolationType type) {
        SlaClockState current = clock(type);
        if (current == SlaClockState.VIOLATED || current == SlaClockState.MET) return false;
        setClock(type, SlaClockState.VIOLATED);
        return true;
    }

    private void meetClock(ViolationType type, Instant at, Instant due) {
        SlaClockState current = clock(type);
        if (current == SlaClockState.VIOLATED) return;
        if (due == null || !at.isAfter(due)) setClock(type, SlaClockState.MET);
    }

    private void setClock(ViolationType type, SlaClockState value) {
        if (type == ViolationType.RESPONSE) responseSla = value; else resolutionSla = value;
        recomputeSlaStatus();
    }

    private void recomputeSlaStatus() {
        if (responseSla == SlaClockState.VIOLATED || resolutionSla == SlaClockState.VIOLATED) {
            slaStatus = SlaStatus.VIOLATED;
        } else if (status != null && status.isOpen() && status != IncidentStatus.VERIFIED
                && (responseSla == SlaClockState.APPROACHING || resolutionSla == SlaClockState.APPROACHING)) {
            slaStatus = SlaStatus.AT_RISK;
        } else {
            slaStatus = SlaStatus.OK;
        }
    }

    /** Escalation chain bookkeeping: true when the level actually increased. */
    public boolean escalateTo(int level) {
        if (level <= escalationLevel) return false;
        escalationLevel = level;
        return true;
    }

    // ------------------------------------------------------------------ correlation and duplicates

    public void linkToGroup(String group) { this.correlationGroup = group; }

    public void markDuplicateOf(String existingPublicId) { this.duplicateOf = existingPublicId; }

    // ------------------------------------------------------------------ composition: notes, chat, feedback

    public IncidentNote addNote(PortalUser author, NoteType type, String content, Instant at) {
        String clean = content == null ? "" : content.strip();
        if (clean.length() < 2) throw ValidationException.field("content", "Add a little more detail before saving.");
        if (clean.length() > 2000) throw ValidationException.field("content", "Notes are limited to 2,000 characters.");
        if (status == IncidentStatus.CLOSED) {
            throw new InvalidStateTransitionException(status.name(), "add a note", "Closed incidents are read-only.");
        }
        IncidentNote note = new IncidentNote(this, author.getId(), author.getName(), type, clean, at);
        notes.add(note);
        return note;
    }

    public ChatMessage addChatMessage(PortalUser sender, String content, Instant at) {
        String clean = content == null ? "" : content.strip();
        if (clean.isEmpty() || clean.length() > 2000) {
            throw ValidationException.field("content", "Message must contain 1 to 2,000 characters.");
        }
        if (!state().allowsConversation()) {
            throw new InvalidStateTransitionException(status.name(), "send a message",
                    "This incident is closed; its conversation is read-only.");
        }
        ChatMessage message = new ChatMessage(this, sender.getId(), sender.getName(), clean, at);
        chatMessages.add(message);
        return message;
    }

    /** Builds the reporter's single satisfaction rating; persistence is done by the caller's repository. */
    public Feedback leaveFeedback(PortalUser employee, int rating, String comment, boolean alreadyGiven, Instant at) {
        if (!employee.getId().equals(reporterId)) {
            throw new UnauthorizedOperationException("Only the employee who reported the incident can leave feedback.");
        }
        if (!state().allowsFeedback()) {
            throw new InvalidStateTransitionException(status.name(), "leave feedback",
                    "Feedback can be given once the incident is resolved.");
        }
        if (alreadyGiven) throw new ConflictException("Feedback has already been submitted for " + publicId + ".");
        if (rating < 1 || rating > 5) throw ValidationException.field("rating", "Rating must be between 1 and 5.");
        String clean = comment == null ? "" : comment.strip();
        if (clean.length() > 1000) throw ValidationException.field("comment", "Comments are limited to 1,000 characters.");
        return new Feedback(this, employee.getId(), rating, clean, at);
    }

    private void resetApprovals() {
        technicianApproved = false;
        employeeApproved = false;
    }

    // ------------------------------------------------------------------ validation helpers

    private static void validateText(String title, String description) {
        String cleanTitle = title == null ? "" : title.strip();
        String cleanDescription = description == null ? "" : description.strip();
        if (cleanTitle.length() < TITLE_MIN || cleanTitle.length() > TITLE_MAX) {
            throw ValidationException.field("title", "Title must be between 5 and 120 characters.");
        }
        if (cleanDescription.length() < DESCRIPTION_MIN || cleanDescription.length() > DESCRIPTION_MAX) {
            throw ValidationException.field("description", "Description must be between 10 and 2,000 characters.");
        }
    }

    private static int requireScale(int value, String field) {
        if (value < 1 || value > 4) throw ValidationException.field(field, "Values must be between 1 and 4.");
        return value;
    }

    // ------------------------------------------------------------------ getters (no setters)

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public String getTechnicianId() { return technicianId; }
    public String getTechnicianName() { return technicianName; }
    public String getCategory() { return category; }
    public int getClassificationConfidence() { return classificationConfidence; }
    public String getServiceName() { return serviceName; }
    public int getImpact() { return impact; }
    public int getUrgency() { return urgency; }
    public int getCriticality() { return criticality; }
    public int getAffectedUsers() { return affectedUsers; }
    public int getPriorityScore() { return priorityScore; }
    public Priority getPriority() { return priority; }
    public String getPriorityStrategy() { return priorityStrategy; }
    public IncidentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClassifiedAt() { return classifiedAt; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getResponseDueAt() { return responseDueAt; }
    public Instant getResolutionDueAt() { return resolutionDueAt; }
    public SlaClockState getResponseSla() { return responseSla; }
    public SlaClockState getResolutionSla() { return resolutionSla; }
    public SlaStatus getSlaStatus() { return slaStatus; }
    public boolean isResponseSlaViolated() { return responseSla == SlaClockState.VIOLATED; }
    public boolean isResolutionSlaViolated() { return resolutionSla == SlaClockState.VIOLATED; }
    public boolean isSlaViolated() { return slaStatus == SlaStatus.VIOLATED; }
    public int getEscalationLevel() { return escalationLevel; }
    public String getResolutionSummary() { return resolutionSummary; }
    public String getRootCause() { return rootCause; }
    public String getResolutionSteps() { return resolutionSteps; }
    public boolean isTechnicianApproved() { return technicianApproved; }
    public boolean isEmployeeApproved() { return employeeApproved; }
    public String getCorrelationGroup() { return correlationGroup; }
    public String getDuplicateOf() { return duplicateOf; }
    public long getVersion() { return version; }
    public boolean isOpen() { return status.isOpen(); }
    public List<IncidentNote> getNotes() { return Collections.unmodifiableList(notes); }
    public List<ChatMessage> getChatMessages() { return Collections.unmodifiableList(chatMessages); }
    public Set<String> getDeclinedBy() { return Collections.unmodifiableSet(declinedBy); }

    /** Legacy text view of the notes ("timestamp | content" per line) kept for the existing UI. */
    public String getNotesTranscript() {
        return notes.stream().map(n -> n.getCreatedAt() + " | " + n.getContent()).collect(Collectors.joining("\n"));
    }
}
