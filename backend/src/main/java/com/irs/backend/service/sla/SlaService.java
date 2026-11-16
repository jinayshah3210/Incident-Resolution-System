package com.irs.backend.service.sla;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import com.irs.backend.model.Incident;
@Service
public class SlaService {

    private final SlaPolicy slaPolicy;

    public SlaService(SlaPolicy slaPolicy) {
        this.slaPolicy = slaPolicy;
    }

    public LocalDateTime calculateResponseDeadline(Incident incident) {
        return incident.getCreatedAt()
                .plus(slaPolicy.getResponseTime(incident.getPriority()));
    }

    public LocalDateTime calculateResolutionDeadline(Incident incident) {
        return incident.getCreatedAt()
                .plus(slaPolicy.getResolutionTime(incident.getPriority()));
    }
}