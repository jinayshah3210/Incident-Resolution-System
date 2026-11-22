package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: ACKNOWLEDGED may start work, or the admin may remove the technician. */
public final class AcknowledgedState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.ACKNOWLEDGED; }
    @Override public IncidentStatus start() { return IncidentStatus.IN_PROGRESS; }
    @Override public IncidentStatus release() { return IncidentStatus.CLASSIFIED; }
}
