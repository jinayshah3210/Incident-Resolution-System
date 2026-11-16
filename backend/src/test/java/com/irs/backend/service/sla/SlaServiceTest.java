package com.irs.backend.service.sla;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.Priority;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SlaServiceTest {

    // Test P1 deadlines
    @Test
    void testP1Deadlines() {
        testPriorityDeadlines(Priority.P1, 15, 4);
    }

    // Test P2 deadlines
    @Test
    void testP2Deadlines() {
        testPriorityDeadlines(Priority.P2, 30, 8);
    }

    // Test P3 deadlines
    @Test
    void testP3Deadlines() {
        testPriorityDeadlines(Priority.P3, 240, 24);
    }

    // Test P4 deadlines
    @Test
    void testP4Deadlines() {
        testPriorityDeadlines(Priority.P4, 480, 72);
    }

    // Common method to test deadlines
    private void testPriorityDeadlines(
            Priority priority,
            long responseMinutes,
            long resolutionHours) {

        Incident incident = new Incident(
                "Test Incident",
                "Testing SLA",
                "NETWORK");

        incident.setPriority(priority);

        SlaService service = new SlaService(new SlaPolicy());

        service.setDeadlines(incident);

        assertEquals(
                responseMinutes,
                Duration.between(
                        incident.getCreatedAt(),
                        incident.getResponseDeadline()).toMinutes());

        assertEquals(
                resolutionHours,
                Duration.between(
                        incident.getCreatedAt(),
                        incident.getResolutionDeadline()).toHours());
    }

    // Test response SLA: responded on time
    @Test
    void testResponseSlaNotBreached() {

        Incident incident = createP1Incident();

        incident.setRespondedAt(
                incident.getResponseDeadline().minusMinutes(5));

        SlaService service = new SlaService(new SlaPolicy());

        assertFalse(service.isResponseSlaBreached(incident));
    }

    // Test response SLA: responded late
    @Test
    void testResponseSlaBreached() {

        Incident incident = createP1Incident();

        incident.setRespondedAt(
                incident.getResponseDeadline().plusMinutes(5));

        SlaService service = new SlaService(new SlaPolicy());

        assertTrue(service.isResponseSlaBreached(incident));
    }

    // Helper method to create P1 incident
    private Incident createP1Incident() {

        Incident incident = new Incident(
                "Server Down",
                "Main server not responding",
                "NETWORK");

        incident.setPriority(Priority.P1);

        SlaService service = new SlaService(new SlaPolicy());

        service.setDeadlines(incident);

        return incident;
    }

    @Test
    void testResponseSlaBreachedWhenNotResponded() {

        Incident incident = createP1Incident();

        // Simulate a deadline that has already passed
        incident.setResponseDeadline(
                LocalDateTime.now().minusMinutes(5));

        // No response has been recorded
        incident.setRespondedAt(null);

        SlaService service = new SlaService(new SlaPolicy());

        assertTrue(service.isResponseSlaBreached(incident));
    }

    @Test
    void testResolutionSlaBreached() {

        Incident incident = createP1Incident();

        incident.setResolutionDeadline(
                LocalDateTime.now().minusHours(1));

        SlaService service = new SlaService(new SlaPolicy());

        assertTrue(service.isResolutionSlaBreached(incident));
    }

    @Test
    void testResolutionSlaNotBreachedWhenResolved() {

        Incident incident = createP1Incident();

        incident.setStatus(
                com.irs.backend.model.enums.IncidentStatus.RESOLVED);

        SlaService service = new SlaService(new SlaPolicy());

        assertFalse(service.isResolutionSlaBreached(incident));
    }

    @Test
    void testResolutionSlaBreachedWhenResolvedLate() {

        Incident incident = createP1Incident();

        incident.setResolvedAt(
                incident.getResolutionDeadline().plusHours(2));

        SlaService service = new SlaService(new SlaPolicy());

        assertTrue(service.isResolutionSlaBreached(incident));
    }

    @Test
    void testResolutionSlaNotBreachedWhenResolvedOnTime() {

        Incident incident = createP1Incident();

        incident.setResolvedAt(
                incident.getResolutionDeadline().minusMinutes(30));

        SlaService service = new SlaService(new SlaPolicy());

        assertFalse(service.isResolutionSlaBreached(incident));
    }
}