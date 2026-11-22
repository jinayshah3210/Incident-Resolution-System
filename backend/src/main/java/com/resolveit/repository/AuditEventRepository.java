package com.resolveit.repository;

import com.resolveit.domain.audit.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Repository pattern, append-only: extends the bare Repository marker so no update or delete
 * method exists. Only save and read operations are declared.
 */
public interface AuditEventRepository extends Repository<AuditEvent, String> {
    AuditEvent save(AuditEvent event);
    long count();
    List<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
    List<AuditEvent> findAllByOrderByOccurredAtDesc();
    List<AuditEvent> findByIncidentIdIgnoreCaseOrderByOccurredAtDesc(String incidentId);
    List<AuditEvent> findByIncidentIdIgnoreCaseOrderByOccurredAtAsc(String incidentId);
    List<AuditEvent> findByIncidentIdIgnoreCaseAndAction(String incidentId, String action);
}
