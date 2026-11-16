
package com.irs.backend.service;

import org.springframework.stereotype.Service;

import com.irs.backend.model.Incident;
import com.irs.backend.model.User;
import com.irs.backend.model.IncidentAuditLog;
import com.irs.backend.repository.IncidentAuditRepository;

@Service
public class AuditService {

    private final IncidentAuditRepository auditRepository;

    public AuditService(IncidentAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void log(
            Incident incident,
            User user,
            String action,
            String previousStatus,
            String newStatus,
            String description) {

        IncidentAuditLog log = new IncidentAuditLog(
                incident,
                user,
                action,
                previousStatus,
                newStatus,
                description
        );

        auditRepository.save(log);
    }
}
