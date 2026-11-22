package com.resolveit.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.sla.SlaClockState;
import com.resolveit.domain.sla.SlaStatus;
import com.resolveit.escalation.EscalationService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SLA warnings and violations (idempotent) and the escalation Chain of Responsibility, driven by a pinned clock. */
class SlaEscalationIntegrationTest extends IntegrationTestBase {
    private static final Instant START = Instant.parse("2026-11-20T09:00:00Z");
    @Autowired EscalationService escalationService;

    @BeforeEach void pinClock() { clock.setInstant(START); }

    @Test void t06WarningAt80PercentThenExactlyOneViolationRow() {
        Incident i = payment();
        sla.evaluate(i, START.plusSeconds(4 * 60 + 5));     // 81% of the 5-minute response window
        assertEquals(SlaClockState.APPROACHING, i.getResponseSla());
        assertEquals(SlaStatus.AT_RISK, i.getSlaStatus());
        assertEquals(1, auditCount(i, "SLA_WARNING"));
        sla.evaluate(i, START.plusSeconds(6 * 60));
        assertEquals(SlaClockState.VIOLATED, i.getResponseSla());
        assertEquals(1, violations.countByIncident(i));
    }

    @Test void t07RunningTheMonitorTwiceNeverDuplicates() {
        Incident i = payment();
        clock.setInstant(START.plusSeconds(40 * 60));
        sla.monitorOpenIncidents();
        long notes = notifications.count();
        long rows = violations.countByIncident(i);
        sla.monitorOpenIncidents();
        assertEquals(2, rows, "response and resolution violations");
        assertEquals(rows, violations.countByIncident(i));
        assertEquals(notes, notifications.count());
        assertEquals(1, auditCount(i, "RESPONSE_SLA_VIOLATED"));
        assertEquals(1, auditCount(i, "RESOLUTION_SLA_VIOLATED"));
    }

    @Test void acknowledgingInTimeMeetsTheResponseClock() {
        Incident i = payment();
        clock.setInstant(START.plusSeconds(60));
        act(i.getTechnicianId(), i, "accept", Map.of());
        assertEquals(SlaClockState.MET, i.getResponseSla());
        sla.evaluate(i, START.plusSeconds(10 * 60));
        assertNotEquals(SlaClockState.VIOLATED, i.getResponseSla());
    }

    @Test void t22UnacknowledgedP1EscalatesOnceThenClimbsTheChain() {
        Incident i = payment();
        escalationService.evaluate(i, START.plusSeconds(6 * 60));
        assertEquals(1, i.getEscalationLevel());
        escalationService.evaluate(i, START.plusSeconds(6 * 60 + 10));
        assertEquals(1, escalations.findByIncidentOrderByLevelAsc(i).size(), "idempotent at the same level");
        escalationService.evaluate(i, START.plusSeconds(11 * 60));
        assertEquals(2, i.getEscalationLevel());
        escalationService.evaluate(i, START.plusSeconds(60 * 60));
        assertEquals(4, i.getEscalationLevel());
        var levels = escalations.findByIncidentOrderByLevelAsc(i);
        assertEquals(java.util.List.of("TECHNICIAN", "TEAM_LEAD", "ADMIN", "IT_MANAGER"),
                levels.stream().map(e -> e.getTargetRole()).toList());
        assertFalse(notifications.findByUserIdOrderByCreatedAtDesc("A004").isEmpty(), "IT manager notified");
        assertEquals(4, auditCount(i, "ESCALATED"));
    }

    @Test void acknowledgedIncidentsAreNotEscalated() {
        Incident i = payment();
        act(i.getTechnicianId(), i, "accept", Map.of());
        escalationService.evaluate(i, START.plusSeconds(60 * 60));
        assertEquals(0, i.getEscalationLevel());
    }

    @Test void dismissingAViolationHidesItButKeepsTheRow() {
        Incident i = payment();
        sla.evaluate(i, START.plusSeconds(40 * 60));
        assertEquals(1, sla.openViolations(user("A001")).size());
        sla.dismiss(user("A001"), i.getPublicId());
        assertTrue(sla.openViolations(user("A001")).isEmpty());
        assertEquals(2, violations.countByIncident(i));
    }
}
