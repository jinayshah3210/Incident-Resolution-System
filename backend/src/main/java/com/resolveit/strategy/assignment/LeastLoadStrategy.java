package com.resolveit.strategy.assignment;

import com.resolveit.domain.incident.Incident;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Strategy pattern: lowest current load wins; ties go to more experience, then the lowest id. */
@Component
public class LeastLoadStrategy implements AssignmentStrategy {
    public static final String KEY = "LEAST_LOAD";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Least workload"; }

    @Override
    public Optional<TechnicianCandidate> select(Incident incident, List<TechnicianCandidate> eligible) {
        return eligible.stream().min(Comparator.comparingLong(TechnicianCandidate::load)
                .thenComparing(c -> -c.technician().getExperienceYears())
                .thenComparing(TechnicianCandidate::id));
    }
}
