package com.resolveit.strategy.assignment;

import com.resolveit.domain.user.Technician;

/** An eligible technician together with the facts strategies rank on. */
public record TechnicianCandidate(Technician technician, long load, long pastIncidentsOnService) {
    public String id() { return technician.getId(); }
}
