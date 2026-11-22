package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: CLASSIFIED can only be offered to a technician. */
public final class ClassifiedState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.CLASSIFIED; }
    @Override public IncidentStatus offer() { return IncidentStatus.ASSIGNED; }
}
