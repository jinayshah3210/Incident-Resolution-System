package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: RESOLVED waits for both approvals; a rejection reopens work. */
public final class ResolvedState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.RESOLVED; }
    @Override public IncidentStatus verify() { return IncidentStatus.VERIFIED; }
    @Override public IncidentStatus reopen() { return IncidentStatus.IN_PROGRESS; }
    @Override public boolean acceptsClosureReview() { return true; }
    @Override public boolean allowsFeedback() { return true; }
}
