package com.resolveit.domain.notification;

/** Kinds of notification, used for icons and filtering in the bell panel. */
public enum NotificationType {
    INCIDENT_CREATED, ASSIGNMENT_REQUEST, TECHNICIAN_ASSIGNED, STATUS_CHANGED, PRIORITY_CHANGED,
    INFO_REQUEST, MESSAGE, SLA_WARNING, SLA_VIOLATED, CLOSURE, INCIDENT_CLOSED, ESCALATION, ADMIN_ALERT,
    MAJOR_INCIDENT, FEEDBACK
}
