package com.resolveit.service;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.event.IncidentEvent;
import com.resolveit.event.IncidentEventPublisher;
import com.resolveit.event.IncidentEventType;
import com.resolveit.repository.IncidentRepository;
import com.resolveit.web.dto.SuggestionDto;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Intelligent features built on SimilarityService: duplicate blocking, related-incident correlation, suggested resolutions. */
@Service
public class IncidentCorrelationService {
    public static final Duration CORRELATION_WINDOW = Duration.ofMinutes(60);
    public static final int MAJOR_INCIDENT_SIZE = 3;
    private final IncidentRepository incidents;
    private final SimilarityService similarity;
    private final IncidentEventPublisher events;

    public IncidentCorrelationService(IncidentRepository incidents, SimilarityService similarity, IncidentEventPublisher events) {
        this.incidents = incidents;
        this.similarity = similarity;
        this.events = events;
    }

    /** Same reporter, open, similarity >= 0.85. */
    public Optional<Incident> findDuplicate(String reporterId, String title, String description) {
        String text = title + " " + description;
        return incidents.findByReporterIdAndStatusNot(reporterId, IncidentStatus.CLOSED).stream()
                .filter(prior -> similarity.similarity(text, prior.getTitle() + " " + prior.getDescription()) >= SimilarityService.DUPLICATE_THRESHOLD)
                .findFirst();
    }

    /**
     * Different reporters, same service, created within 60 minutes, similarity >= 0.5: the incidents share a
     * correlation group. When a group reaches three incidents the administrators are told about a possible major incident.
     */
    public void correlate(Incident incident, Instant at) {
        String text = incident.getTitle() + " " + incident.getDescription();
        Instant windowStart = incident.getCreatedAt().minus(CORRELATION_WINDOW);
        Incident best = null;
        double bestScore = 0;
        for (Incident other : incidents.findByServiceNameIgnoreCaseAndCreatedAtAfter(incident.getServiceName(), windowStart)) {
            if (other.getId().equals(incident.getId()) || other.getReporterId().equals(incident.getReporterId()) || !other.isOpen()) continue;
            double score = similarity.similarity(text, other.getTitle() + " " + other.getDescription());
            if (score >= SimilarityService.RELATED_THRESHOLD && score > bestScore) {
                best = other;
                bestScore = score;
            }
        }
        if (best == null) return;
        String group = best.getCorrelationGroup() != null ? best.getCorrelationGroup() : best.getPublicId();
        best.linkToGroup(group);
        incident.linkToGroup(group);
        incidents.flush();
        events.publish(IncidentEvent.of(IncidentEventType.RELATED_LINKED, incident, IncidentEvent.SYSTEM,
                String.format("Related to %s (similarity %.2f); correlation group %s.", best.getPublicId(), bestScore, group), null, at));
        long size = incidents.findByCorrelationGroup(group).size();
        if (size == MAJOR_INCIDENT_SIZE) {
            events.publish(IncidentEvent.of(IncidentEventType.MAJOR_INCIDENT_SUSPECTED, incident, IncidentEvent.SYSTEM,
                    size + " similar reports from different employees on " + incident.getServiceName()
                            + " within an hour (group " + group + "). Possible major incident.", null, at));
        }
    }

    public List<Incident> related(Incident incident) {
        if (incident.getCorrelationGroup() == null) return List.of();
        return incidents.findByCorrelationGroup(incident.getCorrelationGroup()).stream()
                .filter(i -> !i.getId().equals(incident.getId()))
                .sorted(Comparator.comparing(Incident::getCreatedAt)).toList();
    }

    /** Up to three resolutions from CLOSED incidents of the same category with similarity >= 0.4. */
    public List<SuggestionDto> suggestResolutions(Incident incident) {
        String text = incident.getTitle() + " " + incident.getDescription();
        return incidents.findByCategoryIgnoreCaseAndStatus(incident.getCategory(), IncidentStatus.CLOSED).stream()
                .filter(c -> !c.getId().equals(incident.getId()) && c.getResolutionSummary() != null)
                .map(c -> new SuggestionDto(c.getPublicId(), c.getTitle(), c.getResolutionSummary(), c.getRootCause(),
                        c.getResolutionSteps(), Math.round(similarity.similarity(text, c.getTitle() + " " + c.getDescription()) * 100) / 100.0))
                .filter(s -> s.similarity() >= SimilarityService.SUGGESTION_THRESHOLD)
                .sorted(Comparator.comparingDouble(SuggestionDto::similarity).reversed())
                .limit(3).toList();
    }
}
