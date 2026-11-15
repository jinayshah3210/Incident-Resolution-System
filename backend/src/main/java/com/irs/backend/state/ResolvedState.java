package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.exception.InvalidIncidentStateException;

public class ResolvedState implements IncidentState {

    @Override
    public void assign(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot assign a resolved incident");
    }

    @Override
    public void startProgress(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot start progress on a resolved incident");
    }

    @Override
    public void putOnHold(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot put a resolved incident on hold");
    }

    @Override
    public void resolve(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already resolved");
    }

    @Override
    public void close(Incident incident) {
        incident.setStatus(IncidentStatus.CLOSED);
    }

    @Override
    public void reopen(Incident incident) {
        incident.setStatus(IncidentStatus.REOPENED);
    }
}