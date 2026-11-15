package com.irs.backend.state;

import com.irs.backend.model.Incident;

public interface IncidentState {

    void assign(Incident incident);

    void startProgress(Incident incident);

    void putOnHold(Incident incident);

    void resolve(Incident incident);

    void close(Incident incident);

    void reopen(Incident incident);

}