package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: ASSIGNED may be accepted, or rejected back to CLASSIFIED. */
public final class AssignedState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.ASSIGNED; }
    @Override public IncidentStatus acknowledge() { return IncidentStatus.ACKNOWLEDGED; }
    @Override public IncidentStatus release() { return IncidentStatus.CLASSIFIED; }
}
