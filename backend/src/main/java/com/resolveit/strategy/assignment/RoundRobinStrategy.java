package com.resolveit.strategy.assignment;

import com.resolveit.domain.incident.Incident;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Strategy pattern: rotates through eligible technicians (sorted by id), one cursor per service. */
@Component
public class RoundRobinStrategy implements AssignmentStrategy {
    public static final String KEY = "ROUND_ROBIN";
    private final Map<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Round robin"; }

    @Override
    public Optional<TechnicianCandidate> select(Incident incident, List<TechnicianCandidate> eligible) {
        if (eligible.isEmpty()) return Optional.empty();
        List<TechnicianCandidate> ordered = eligible.stream().sorted(Comparator.comparing(TechnicianCandidate::id)).toList();
        String scope = incident.getServiceName() == null ? "*" : incident.getServiceName().toLowerCase(Locale.ROOT);
        int index = Math.floorMod(cursors.computeIfAbsent(scope, k -> new AtomicInteger()).getAndIncrement(), ordered.size());
        return Optional.of(ordered.get(index));
    }
}
