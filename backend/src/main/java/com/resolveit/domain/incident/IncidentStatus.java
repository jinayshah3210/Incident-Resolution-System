package com.resolveit.domain.incident;

import java.util.EnumSet;
import java.util.Set;

/** The eight lifecycle states. Stored as STRING so existing database values stay readable. */
public enum IncidentStatus {
    NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED;

    /** States that count toward a technician's workload (a pending offer counts, preventing double booking). */
    public static final Set<IncidentStatus> WORKLOAD = EnumSet.of(ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, RESOLVED);
    /** States in which the response clock has stopped because the technician accepted. */
    public static final Set<IncidentStatus> RESPONDED = EnumSet.of(ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED, CLOSED);

    public boolean isOpen() { return this != CLOSED; }
    public boolean countsTowardWorkload() { return WORKLOAD.contains(this); }
    public boolean hasResponded() { return RESPONDED.contains(this); }
}
