package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;

/** State pattern: CLOSED is terminal; every action is rejected. */
public final class ClosedState implements IncidentState {
    @Override public IncidentStatus status() { return IncidentStatus.CLOSED; }
    @Override public boolean allowsConversation() { return false; }
    @Override public boolean allowsFeedback() { return true; }
}
