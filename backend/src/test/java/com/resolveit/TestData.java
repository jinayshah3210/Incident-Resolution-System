package com.resolveit;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.user.Employee;
import com.resolveit.domain.user.Technician;
import java.time.Instant;
import java.util.Set;

/** Shared fixtures for unit tests (no Spring context). */
public final class TestData {
    public static final Employee REPORTER = new Employee("E101", "Jordan Lee", "e101@example.test", "Password@123", "Finance");
    public static final Technician TECH = new Technician("T201", "Priya Shah", "t201@example.test", "Password@123", "Support",
            Set.of("Application", "Software"), 8, 6, false);
    public static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");

    private TestData() { }

    public static Incident newIncident() {
        return new Incident(9999, "Payment application unavailable",
                "Payment application is unavailable and users are unable to complete transactions.", REPORTER, 3, T0);
    }

    /** Walks a fresh incident along the legal path until it reaches the requested status. */
    public static Incident incidentIn(IncidentStatus target) {
        Incident i = newIncident();
        if (target == IncidentStatus.NEW) return i;
        i.classify("Application", 90, "Payment", 4, T0);
        if (target == IncidentStatus.CLASSIFIED) return i;
        i.offerTo(TECH, T0.plusSeconds(10));
        if (target == IncidentStatus.ASSIGNED) return i;
        i.acknowledge(T0.plusSeconds(20));
        if (target == IncidentStatus.ACKNOWLEDGED) return i;
        i.startWork(T0.plusSeconds(30));
        if (target == IncidentStatus.IN_PROGRESS) return i;
        i.resolve("Restarted the gateway", "Pool exhausted", "Restart", T0.plusSeconds(40));
        if (target == IncidentStatus.RESOLVED) return i;
        i.approveClosure(true, T0.plusSeconds(50));
        i.approveClosure(false, T0.plusSeconds(60));
        return i;
    }
}
