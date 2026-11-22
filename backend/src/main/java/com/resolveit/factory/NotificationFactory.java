package com.resolveit.factory;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.notification.Notification;
import com.resolveit.domain.notification.NotificationType;
import com.resolveit.domain.incident.Priority;
import com.resolveit.domain.user.AdminLevel;
import com.resolveit.domain.user.Administrator;
import com.resolveit.event.IncidentEvent;
import com.resolveit.repository.AdministratorRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: builds role-specific, human-readable notifications for an event.
 * This class is the single place where the recipient matrix (who hears about what) is defined.
 */
@Component
public class NotificationFactory {
    private final AdministratorRepository administrators;

    public NotificationFactory(AdministratorRepository administrators) { this.administrators = administrators; }

    public Notification create(String recipientId, NotificationType type, Incident incident, String title, String message, Instant at) {
        return new Notification(recipientId, incident == null ? null : incident.getPublicId(), type, title, message, at);
    }

    public List<Notification> forEvent(IncidentEvent event) {
        Incident i = event.incident();
        String ref = i.getPublicId() + " \"" + i.getTitle() + "\"";
        Instant at = event.occurredAt();
        List<Notification> out = new ArrayList<>();
        switch (event.type()) {
            case INCIDENT_CREATED -> {
                add(out, i.getReporterId(), NotificationType.INCIDENT_CREATED, i, "Incident received",
                        "We logged " + ref + ". You can follow its progress under Ongoing incidents.", at);
            }
            case PRIORITY_CALCULATED -> {
                if (i.getPriority() != null && i.getPriority().isAtLeast(Priority.P2)) {
                    admins(out, NotificationType.ADMIN_ALERT, i, "Incident needs attention",
                            i.getPriority() + " incident " + ref + " was reported for " + i.getServiceName() + ".", at);
                }
            }
            case OFFER_CREATED, INCIDENT_REASSIGNED -> add(out, i.getTechnicianId(), NotificationType.ASSIGNMENT_REQUEST, i,
                    "New incident assigned", "Review and accept " + ref + " (" + i.getPriority() + ", " + i.getCategory() + ").", at);
            case OFFER_ACCEPTED -> {
                add(out, i.getReporterId(), NotificationType.TECHNICIAN_ASSIGNED, i, "Technician assigned",
                        i.getTechnicianName() + " (" + i.getCategory() + ") accepted " + ref + ".", at);
                if (i.getPriority() == Priority.P1) {
                    admins(out, NotificationType.ADMIN_ALERT, i, "Critical incident assigned",
                            "P1 " + ref + " was accepted by " + i.getTechnicianName() + ".", at);
                }
            }
            case INCIDENT_UNASSIGNABLE -> admins(out, NotificationType.ADMIN_ALERT, i, "Incident is unassigned",
                    ref + " has no eligible technician. " + nullSafe(event.detail()), at);
            case TECHNICIAN_REMOVED -> add(out, event.attribute("previousTechnician"), NotificationType.STATUS_CHANGED, i,
                    "Incident removed from your queue", ref + " was taken off your queue by an administrator.", at);
            case WORK_STARTED -> add(out, i.getReporterId(), NotificationType.STATUS_CHANGED, i, "Work has started",
                    i.getTechnicianName() + " started investigating " + ref + ".", at);
            case INFO_REQUESTED -> add(out, i.getReporterId(), NotificationType.INFO_REQUEST, i, "More information requested",
                    i.getTechnicianName() + " needs more details on " + ref + ": " + nullSafe(event.detail()), at);
            case INFO_PROVIDED -> add(out, i.getTechnicianId(), NotificationType.INFO_REQUEST, i, "Employee added information",
                    i.getReporterName() + " added details to " + ref + ".", at);
            case CHAT_MESSAGE_SENT -> add(out, event.attribute("recipient"), NotificationType.MESSAGE, i, "New incident message",
                    nullSafe(event.attribute("senderName")) + " sent an update on " + i.getPublicId() + ".", at);
            case INCIDENT_RESOLVED -> add(out, i.getReporterId(), NotificationType.STATUS_CHANGED, i,
                    "Your incident is ready to review", i.getTechnicianName() + " resolved " + ref + ": "
                            + nullSafe(i.getResolutionSummary()) + " Please approve or reject the closure.", at);
            case CLOSURE_APPROVED -> add(out, event.attribute("recipient"), NotificationType.CLOSURE, i, "Closure approved by the other party",
                    nullSafe(event.attribute("actorName")) + " approved closing " + ref + ". Your approval is still needed.", at);
            case CLOSURE_REJECTED -> add(out, event.attribute("recipient"), NotificationType.CLOSURE, i, "Closure rejected",
                    nullSafe(event.attribute("actorName")) + " said " + ref + " is not fixed yet; work has resumed.", at);
            case INCIDENT_CLOSED -> {
                add(out, i.getReporterId(), NotificationType.INCIDENT_CLOSED, i, "Incident closed",
                        ref + " is closed. You can rate the support you received under Past incidents.", at);
                add(out, i.getTechnicianId(), NotificationType.INCIDENT_CLOSED, i, "Incident closed",
                        ref + " was verified by both sides and closed.", at);
            }
            case PRIORITY_CHANGED -> {
                add(out, i.getReporterId(), NotificationType.PRIORITY_CHANGED, i, "Priority updated",
                        ref + " is now " + i.getPriority() + ". " + nullSafe(event.detail()), at);
                add(out, i.getTechnicianId(), NotificationType.PRIORITY_CHANGED, i, "Priority updated",
                        ref + " is now " + i.getPriority() + ". " + nullSafe(event.detail()), at);
            }
            case SLA_WARNING -> {
                String target = i.getTechnicianId() != null ? i.getTechnicianId() : null;
                if (target != null) add(out, target, NotificationType.SLA_WARNING, i, "SLA deadline approaching", nullSafe(event.detail()), at);
                else admins(out, NotificationType.SLA_WARNING, i, "SLA deadline approaching", nullSafe(event.detail()), at);
            }
            case RESPONSE_SLA_VIOLATED, RESOLUTION_SLA_VIOLATED -> {
                String clock = event.type().name().startsWith("RESPONSE") ? "response" : "resolution";
                add(out, i.getTechnicianId(), NotificationType.SLA_VIOLATED, i, "Urgent: " + clock + " SLA violated",
                        ref + " passed its " + clock + " deadline. Please act now.", at);
                admins(out, NotificationType.SLA_VIOLATED, i, capitalize(clock) + " SLA violated",
                        (i.getTechnicianName() == null ? "No technician" : "Technician " + i.getTechnicianName())
                                + " did not meet the " + clock + " target for " + ref + ".", at);
                add(out, i.getReporterId(), NotificationType.SLA_VIOLATED, i, "We're running behind",
                        "Sorry, due to certain issues your " + clock + " for " + i.getPublicId() + " is delayed.", at);
            }
            case ESCALATED -> {
                for (String recipient : split(event.attribute("recipients"))) {
                    add(out, recipient, NotificationType.ESCALATION, i, "Escalation: " + i.getPublicId(), nullSafe(event.detail()), at);
                }
            }
            case MAJOR_INCIDENT_SUSPECTED -> admins(out, NotificationType.MAJOR_INCIDENT, i, "Possible major incident",
                    nullSafe(event.detail()), at);
            case SLA_POLICY_FALLBACK -> admins(out, NotificationType.ADMIN_ALERT, i, "SLA policy missing", nullSafe(event.detail()), at);
            case FEEDBACK_SUBMITTED -> add(out, i.getTechnicianId(), NotificationType.FEEDBACK, i, "New feedback received",
                    i.getReporterName() + " rated " + i.getPublicId() + ": " + nullSafe(event.detail()), at);
            default -> { }
        }
        return out;
    }

    private void add(List<Notification> out, String recipient, NotificationType type, Incident incident, String title, String message, Instant at) {
        if (recipient == null || recipient.isBlank()) return;
        out.add(create(recipient, type, incident, title, message, at));
    }

    private void admins(List<Notification> out, NotificationType type, Incident incident, String title, String message, Instant at) {
        for (Administrator admin : administrators.findByAdminLevelAndActiveTrueOrderByIdAsc(AdminLevel.ADMINISTRATOR)) {
            add(out, admin.getId(), type, incident, title, message, at);
        }
    }

    private static Set<String> split(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return new LinkedHashSet<>(Arrays.stream(csv.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList());
    }

    private static String nullSafe(String value) { return value == null ? "" : value; }

    private static String capitalize(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }
}
