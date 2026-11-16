package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.exception.InvalidIncidentStateException;

public class OnHoldState implements IncidentState {

    @Override
    public void assign(Incident incident) {
        throw new InvalidIncidentStateException(
                "Cannot reassign an incident while it is on hold");
    }

    @Override
    public void startProgress(Incident incident) {
        incident.setStatus(IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void putOnHold(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already on hold");
    }

    @Override
    public void resolve(Incident incident) {
        throw new InvalidIncidentStateException(
                "Resume work before resolving the incident");
    }

    @Override
    public void close(Incident incident) {
        throw new InvalidIncidentStateException(
                "Resume and resolve the incident before closing");
    }

    @Override
    public void reopen(Incident incident) {
        throw new InvalidIncidentStateException(
                "Incident is already active");
    }
}