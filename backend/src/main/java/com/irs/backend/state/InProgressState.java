package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.exception.InvalidIncidentStateException;

public class InProgressState implements IncidentState {

    @Override
    public void assign(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already assigned");
    }

    @Override
    public void startProgress(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already in progress");
    }

    @Override
    public void putOnHold(Incident incident) {
        incident.setStatus(IncidentStatus.ON_HOLD);
    }

    @Override
    public void resolve(Incident incident) {
        incident.setStatus(IncidentStatus.RESOLVED);
    }

    @Override
    public void close(Incident incident) {
        throw new InvalidIncidentStateException(
                "Resolve the incident before closing it");
    }

    @Override
    public void reopen(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already active");
    }
}