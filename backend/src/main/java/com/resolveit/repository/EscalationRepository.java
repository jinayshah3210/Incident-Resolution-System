package com.resolveit.repository;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.sla.Escalation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: escalation steps. */
public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    List<Escalation> findByIncidentOrderByLevelAsc(Incident incident);
    boolean existsByIncidentAndLevel(Incident incident, int level);
}
