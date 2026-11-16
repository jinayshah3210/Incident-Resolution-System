package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;

public class IncidentStateManager {

    private IncidentState getState(Incident incident) {

        IncidentStatus status = incident.getStatus();

        return switch (status) {
    case NEW -> new NewState();
    case ASSIGNED, ACKNOWLEDGED -> new AssignedState();
    case IN_PROGRESS -> new InProgressState();
    case ON_HOLD -> new OnHoldState();
    case RESOLVED, VERIFIED -> new ResolvedState();
    case CLOSED -> new ClosedState();
    case REOPENED -> new ReopenedState();
};
    }

    public void assign(Incident incident) {
        getState(incident).assign(incident);
    }

    public void startProgress(Incident incident) {
        getState(incident).startProgress(incident);
    }

    public void putOnHold(Incident incident) {
        getState(incident).putOnHold(incident);
    }

    public void resolve(Incident incident) {
        getState(incident).resolve(incident);
    }

    public void close(Incident incident) {
        getState(incident).close(incident);
    }

    public void reopen(Incident incident) {
        getState(incident).reopen(incident);
    }
}