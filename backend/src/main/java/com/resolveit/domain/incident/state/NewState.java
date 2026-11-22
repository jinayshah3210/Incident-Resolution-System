package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: NEW can only be classified. */
public final class NewState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.NEW; }
    @Override public IncidentStatus classify() { return IncidentStatus.CLASSIFIED; }
}
