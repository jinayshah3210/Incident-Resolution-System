package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.exception.InvalidIncidentStateException;

public class AssignedState implements IncidentState {

    @Override
    public void assign(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already assigned");
    }

    @Override
    public void startProgress(Incident incident) {
        incident.setStatus(IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void putOnHold(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot put an incident on hold before work starts");
    }

    @Override
    public void resolve(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot resolve an incident before work starts");
    }

    @Override
    public void close(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot close an assigned incident");
    }

    @Override
    public void reopen(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot reopen an assigned incident");
    }
}