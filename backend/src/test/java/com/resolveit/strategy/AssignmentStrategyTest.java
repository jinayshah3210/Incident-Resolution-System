package com.resolveit.strategy;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.TestData;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.user.Technician;
import com.resolveit.strategy.assignment.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The three interchangeable assignment strategies. */
class AssignmentStrategyTest {
    private static final Technician A = new Technician("T301", "Alpha", "a@t.test", "Password@123", "Ops", Set.of("Application"), 8, 3, false);
    private static final Technician B = new Technician("T302", "Bravo", "b@t.test", "Password@123", "Ops", Set.of("Network"), 8, 9, false);
    private static final Technician C = new Technician("T303", "Charlie", "c@t.test", "Password@123", "Ops", Set.of("Application"), 8, 9, false);
    private final Incident incident = TestData.incidentIn(IncidentStatus.CLASSIFIED);

    @Test void t10LeastLoadPicksFewestActiveIncidents() {
        var picked = new LeastLoadStrategy().select(incident, List.of(
                new TechnicianCandidate(A, 3, 0), new TechnicianCandidate(B, 1, 0), new TechnicianCandidate(C, 2, 0)));
        assertEquals("T302", picked.orElseThrow().id());
    }

    @Test void t10LeastLoadTieGoesToMoreExperience() {
        var picked = new LeastLoadStrategy().select(incident, List.of(new TechnicianCandidate(A, 1, 0), new TechnicianCandidate(C, 1, 0)));
        assertEquals("T303", picked.orElseThrow().id());
    }

    @Test void t11RoundRobinRotatesAcrossEligibleTechnicians() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();
        List<TechnicianCandidate> eligible = List.of(new TechnicianCandidate(C, 0, 0), new TechnicianCandidate(A, 5, 0));
        assertEquals("T301", strategy.select(incident, eligible).orElseThrow().id());
        assertEquals("T303", strategy.select(incident, eligible).orElseThrow().id());
        assertEquals("T301", strategy.select(incident, eligible).orElseThrow().id());
    }

    @Test void skillExperiencePrefersCategoryMatchThenExperience() {
        var picked = new SkillExperienceStrategy().select(incident, List.of(
                new TechnicianCandidate(B, 0, 0), new TechnicianCandidate(A, 0, 0), new TechnicianCandidate(C, 4, 0)));
        assertEquals("T303", picked.orElseThrow().id());
    }

    @Test void emptyEligibleListSelectsNobody() {
        assertTrue(new LeastLoadStrategy().select(incident, List.of()).isEmpty());
        assertTrue(new RoundRobinStrategy().select(incident, List.of()).isEmpty());
        assertTrue(new SkillExperienceStrategy().select(incident, List.of()).isEmpty());
    }
}
