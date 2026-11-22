package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: IN_PROGRESS may be resolved, or the admin may remove the technician. */
public final class InProgressState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.IN_PROGRESS; }
    @Override public IncidentStatus resolve() { return IncidentStatus.RESOLVED; }
    @Override public IncidentStatus release() { return IncidentStatus.CLASSIFIED; }
}
