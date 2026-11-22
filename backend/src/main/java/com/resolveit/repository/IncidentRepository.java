package com.resolveit.repository;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Repository pattern for the incident aggregate; dynamic search uses JPA Specifications. */
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    Optional<Incident> findByPublicIdIgnoreCase(String publicId);
    List<Incident> findByReporterIdOrderByCreatedAtDesc(String reporterId);
    List<Incident> findByTechnicianIdOrderByCreatedAtDesc(String technicianId);
    List<Incident> findAllByOrderByCreatedAtDesc();
    List<Incident> findByStatusInOrderByCreatedAtAsc(Collection<IncidentStatus> statuses);
    List<Incident> findByStatusNot(IncidentStatus status);
    List<Incident> findByReporterIdAndStatusNot(String reporterId, IncidentStatus status);
    List<Incident> findByServiceNameIgnoreCaseAndCreatedAtAfter(String serviceName, Instant after);
    List<Incident> findByCategoryIgnoreCaseAndStatus(String category, IncidentStatus status);
    List<Incident> findByCorrelationGroup(String correlationGroup);
    List<Incident> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant from, Instant to);
    long countByTechnicianIdAndStatusIn(String technicianId, Collection<IncidentStatus> statuses);
    long countByTechnicianIdAndServiceNameIgnoreCase(String technicianId, String serviceName);
    long countByServiceNameIgnoreCase(String serviceName);
    long countByReporterId(String reporterId);
    long countByReporterIdAndStatus(String reporterId, IncidentStatus status);
    long countByCategoryIgnoreCaseAndStatusNot(String category, IncidentStatus status);

    @Query("select coalesce(max(i.sequenceNumber), 1000) from Incident i")
    long findMaxSequenceNumber();

    @Query("select i.technicianId, count(i) from Incident i where i.technicianId is not null and i.status in :statuses group by i.technicianId")
    List<Object[]> countWorkloadByTechnician(Collection<IncidentStatus> statuses);
}
