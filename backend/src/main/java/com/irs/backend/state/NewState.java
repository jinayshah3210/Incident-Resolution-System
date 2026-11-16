package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.exception.InvalidIncidentStateException;

public class NewState implements IncidentState {

    @Override
    public void assign(Incident incident) {
        incident.setStatus(IncidentStatus.ASSIGNED);
    }

    @Override
    public void startProgress(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot start progress before assignment");
    }

    @Override
    public void putOnHold(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot put a new incident on hold");
    }

    @Override
    public void resolve(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot resolve an unassigned incident");
    }

    @Override
    public void close(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot close a new incident");
    }

    @Override
    public void reopen(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot reopen a new incident");
    }
}