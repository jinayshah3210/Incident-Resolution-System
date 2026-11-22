package com.resolveit.strategy.assignment;

import com.resolveit.domain.incident.Incident;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Strategy pattern: prefers a technician whose skills include the incident category, then the most
 * experienced, then the lowest load. Useful for critical services where expertise matters most.
 */
@Component
public class SkillExperienceStrategy implements AssignmentStrategy {
    public static final String KEY = "SKILL_EXPERIENCE";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Best skill match and experience"; }

    @Override
    public Optional<TechnicianCandidate> select(Incident incident, List<TechnicianCandidate> eligible) {
        Comparator<TechnicianCandidate> bySkill = Comparator.comparing(
                c -> c.technician().canHandle(incident.getCategory()) ? 0 : 1);
        return eligible.stream().min(bySkill
                .thenComparing(c -> -c.technician().getExperienceYears())
                .thenComparingLong(TechnicianCandidate::load)
                .thenComparing(TechnicianCandidate::id));
    }
}
