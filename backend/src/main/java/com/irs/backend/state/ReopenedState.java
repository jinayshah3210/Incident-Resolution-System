package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.exception.InvalidIncidentStateException;

public class ReopenedState implements IncidentState {

    @Override
    public void assign(Incident incident) {
        incident.setStatus(IncidentStatus.ASSIGNED);
    }

    @Override
    public void startProgress(Incident incident) {
        incident.setStatus(IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void putOnHold(Incident incident) {
        throw new InvalidIncidentStateException(
                "Start progress before putting the incident on hold");
    }

    @Override
    public void resolve(Incident incident) {
        throw new InvalidIncidentStateException(
                "Start progress before resolving the incident");
    }

    @Override
    public void close(Incident incident) {
        throw new InvalidIncidentStateException(
                "Resolve the incident before closing it");
    }

    @Override
    public void reopen(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already reopened");
    }
}