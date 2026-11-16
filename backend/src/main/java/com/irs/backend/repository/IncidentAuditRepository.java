
package com.irs.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.irs.backend.model.IncidentAuditLog;

public interface IncidentAuditRepository
        extends JpaRepository<IncidentAuditLog, Long> {

    List<IncidentAuditLog> findByIncidentIdOrderByTimestampAsc(Long incidentId);
}
