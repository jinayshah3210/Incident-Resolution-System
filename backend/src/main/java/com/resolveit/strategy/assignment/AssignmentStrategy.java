package com.resolveit.strategy.assignment;

import com.resolveit.domain.incident.Incident;
import com.resolveit.strategy.NamedStrategy;
import java.util.List;
import java.util.Optional;

/** Strategy pattern: picks one technician from the already-filtered eligible list. */
public interface AssignmentStrategy extends NamedStrategy {
    Optional<TechnicianCandidate> select(Incident incident, List<TechnicianCandidate> eligible);
}
