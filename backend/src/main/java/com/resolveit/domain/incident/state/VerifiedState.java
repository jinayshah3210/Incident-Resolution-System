package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: VERIFIED closes automatically. */
public final class VerifiedState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.VERIFIED; }
    @Override public IncidentStatus close() { return IncidentStatus.CLOSED; }
    @Override public boolean allowsFeedback() { return true; }
}
