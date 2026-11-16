package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.exception.InvalidIncidentStateException;

public class ClosedState implements IncidentState {

    @Override
    public void assign(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot assign a closed incident");
    }

    @Override
    public void startProgress(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot restart a closed incident");
    }

    @Override
    public void putOnHold(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot put a closed incident on hold");
    }

    @Override
    public void resolve(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already closed");
    }

    @Override
    public void close(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already closed");
    }

    @Override
    public void reopen(Incident incident) {
        throw new InvalidIncidentStateException(
                "Closed incidents cannot be reopened directly");
    }
}