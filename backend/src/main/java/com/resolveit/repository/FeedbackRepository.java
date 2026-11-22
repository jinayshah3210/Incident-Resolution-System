package com.resolveit.repository;

import com.resolveit.domain.incident.Feedback;
import com.resolveit.domain.incident.Incident;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository pattern: employee satisfaction ratings. */
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByIncident(Incident incident);
    boolean existsByIncident(Incident incident);
    List<Feedback> findByIncidentIn(Collection<Incident> incidents);
}
