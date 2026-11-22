package com.resolveit.repository;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.sla.SlaViolation;
import com.resolveit.domain.sla.ViolationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: recorded SLA violations. */
public interface SlaViolationRepository extends JpaRepository<SlaViolation, Long> {
    boolean existsByIncidentAndType(Incident incident, ViolationType type);
    List<SlaViolation> findByIncident(Incident incident);
    List<SlaViolation> findByDismissedFalseOrderByDetectedAtDesc();
    long countByIncident(Incident incident);
}
