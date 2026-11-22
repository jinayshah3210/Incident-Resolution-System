package com.resolveit.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.Priority;
import com.resolveit.exception.*;
import com.resolveit.web.dto.AdminIncidentUpdate;
import com.resolveit.web.dto.CreateIncidentRequest;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Incident pipeline, lifecycle, assignment and intelligent features through the real services. */
class IncidentWorkflowIntegrationTest extends IntegrationTestBase {

    @Test void t01CreationRunsTheWholePipeline() {
        Incident i = payment();
        assertTrue(i.getPublicId().matches("INC\\d{4,}"));
        assertEquals("Application", i.getCategory());
        assertEquals("Payment", i.getServiceName());
        assertEquals(Priority.P1, i.getPriority());
        assertEquals(64, i.getPriorityScore());
        assertEquals(IncidentStatus.ASSIGNED, i.getStatus());
        assertNotNull(i.getTechnicianId());
        for (String action : new String[]{"INCIDENT_CREATED", "CLASSIFIED", "PRIORITY_CALCULATED", "OFFER_CREATED"}) {
            assertEquals(1, auditCount(i, action), action);
        }
        assertFalse(notifications.findByUserIdOrderByCreatedAtDesc(i.getTechnicianId()).isEmpty());
    }

    @Test void t05P1DeadlinesAreFiveAndThirtyMinutes() {
        Incident i = payment();
        assertEquals(Duration.ofMinutes(5), Duration.between(i.getCreatedAt(), i.getResponseDueAt()));
        assertEquals(Duration.ofMinutes(30), Duration.between(i.getCreatedAt(), i.getResolutionDueAt()));
    }

    @Test void fullLifecycleClosesWithBothApprovalsAndCompleteAudit() {
        Incident i = payment();
        String tech = i.getTechnicianId();
        act(tech, i, "accept", Map.of());
        act(tech, i, "start", Map.of());
        act(tech, i, "note", Map.of("content", "Gateway pool exhausted"));
        act(tech, i, "resolve", Map.of("summary", "Restarted the gateway", "rootCause", "Pool", "steps", "Restart"));
        act(tech, i, "approve", Map.of());
        assertEquals(IncidentStatus.RESOLVED, i.getStatus());
        act("E101", i, "approve", Map.of());
        assertEquals(IncidentStatus.CLOSED, i.getStatus());
        assertEquals(1, auditCount(i, "INCIDENT_CLOSED"));
        assertEquals(2, auditCount(i, "CLOSURE_APPROVED"));
    }

    @Test void t14OneRejectionReturnsToInProgressAndNotifiesTheOtherParty() {
        Incident i = payment();
        String tech = i.getTechnicianId();
        act(tech, i, "accept", Map.of());
        act(tech, i, "start", Map.of());
        act(tech, i, "resolve", Map.of("summary", "Restarted the gateway"));
        long before = notifications.countByUserId(tech);
        act("E101", i, "reject-closure", Map.of("reason", "Still failing"));
        assertEquals(IncidentStatus.IN_PROGRESS, i.getStatus());
        assertTrue(notifications.countByUserId(tech) > before);
    }

    @Test void t13RejectingAnOfferReassignsToAnotherTechnician() {
        Incident i = payment();
        String first = i.getTechnicianId();
        act(first, i, "reject", Map.of("reason", "On leave"));
        assertEquals(IncidentStatus.ASSIGNED, i.getStatus());
        assertNotEquals(first, i.getTechnicianId());
        assertTrue(i.getDeclinedBy().contains(first));
    }

    @Test void t12NobodyEligibleLeavesIncidentUnassignedAndAlertsAdmins() {
        Incident i = create("E101", "Projector bulb flickering", "The projector bulb flickers in the board room, workaround exists.", "Endpoint / Hardware");
        // every qualified technician declines until the pool is exhausted
        while (i.getTechnicianId() != null) act(i.getTechnicianId(), i, "reject", Map.of());
        assertEquals(IncidentStatus.CLASSIFIED, i.getStatus());
        assertTrue(auditCount(i, "INCIDENT_UNASSIGNABLE") >= 1);
        assertFalse(notifications.findByUserIdOrderByCreatedAtDesc("A001").isEmpty());
    }

    @Test void t20DuplicateIsBlockedUnlessForced() {
        Incident first = payment();
        DuplicateIncidentException e = assertThrows(DuplicateIncidentException.class, this::payment);
        assertEquals(first.getPublicId(), e.getExistingIncidentId());
        Incident forced = workflow.createIncident(user("E101"), new CreateIncidentRequest(PAYMENT_TITLE, PAYMENT_TEXT, null, null, true));
        assertEquals(first.getPublicId(), forced.getDuplicateOf());
    }

    @Test void t21ThreeSimilarReportsFromDifferentEmployeesAreCorrelated() {
        Incident a = create("E101", "VPN connection keeps dropping", "The VPN connection keeps dropping every few minutes for our team.", "Network");
        Incident b = create("E102", "VPN connection dropping", "VPN connection keeps dropping every few minutes for our team.", "Network");
        Incident c = create("E301", "VPN connection keeps dropping again", "The VPN connection keeps dropping every few minutes, team affected.", "Network");
        assertNotNull(a.getCorrelationGroup());
        assertEquals(a.getCorrelationGroup(), b.getCorrelationGroup());
        assertEquals(a.getCorrelationGroup(), c.getCorrelationGroup());
        assertEquals(1, auditCount(c, "MAJOR_INCIDENT_SUSPECTED"));
        assertEquals(2, queries.detail(user("A001"), a.getPublicId()).related().size());
    }

    @Test void t23MissingSlaPolicyFallsBackToDefaults() {
        settingRows.deleteById("sla.P1.response.minutes");
        Incident i = payment();
        assertEquals(Duration.ofMinutes(5), Duration.between(i.getCreatedAt(), i.getResponseDueAt()));
        assertEquals(1, auditCount(i, "SLA_POLICY_FALLBACK"));
    }

    @Test void t24UnknownServiceIs404AndAutoDetectionFallsBackToGeneralIt() {
        assertThrows(ServiceNotFoundException.class, () -> create("E101", "Something broke", "Something broke in a tool nobody knows.", "Teleportation"));
        Incident i = create("E101", "Something odd happened", "Something odd happened with a tool I use.", null);
        assertEquals("General IT", i.getServiceName());
    }

    @Test void t19ShortTitleAndHtmlPayloadAreHandled() {
        assertThrows(ValidationException.class, () -> create("E101", "Bad", "A long enough description here.", null));
        assertThrows(ValidationException.class, () -> create("E101", "Valid title", "Too short", null));
        Incident i = create("E101", "<b>Printer</b> paper jam", "<script>x</script>The printer has a paper jam on floor two.", null);
        assertFalse(i.getTitle().contains("<"));
        assertFalse(i.getDescription().contains("<script>"));
    }

    @Test void weightedStrategySwitchesWithoutRestart() {
        settings.put("priority.strategy", "WEIGHTED");
        Incident i = create("E101", "Service down for all users", "The internal service is down for everyone and blocking work.", "General IT");
        assertEquals("WEIGHTED", i.getPriorityStrategy());
        assertEquals(81, i.getPriorityScore());
        assertEquals(Priority.P2, i.getPriority());
    }

    @Test void roundRobinSettingChangesWhoGetsTheOffer() {
        settings.put("assignment.strategy", "ROUND_ROBIN");
        Incident a = create("E101", "ERP purchase order screen error", "The ERP purchase order screen throws an error.", "ERP");
        Incident b = create("E102", "Payroll run is stuck in SAP", "Our payroll batch in SAP is stuck since this morning.", "ERP");
        assertNotEquals(a.getTechnicianId(), b.getTechnicianId());
    }

    @Test void adminEditRecomputesPriorityAndSla() {
        Incident i = payment();
        var dto = workflow.adminEdit(user("A001"), i.getPublicId(), new AdminIncidentUpdate(null, null, 1, 1, 1, null));
        assertEquals("P4", dto.priority());
        assertEquals(Duration.ofHours(4), Duration.between(i.getCreatedAt(), i.getResponseDueAt()));
        assertEquals(1, auditCount(i, "PRIORITY_CHANGED"));
    }

    @Test void adminAdvanceIsLimitedToTwoStepsAndNeverCloses() {
        Incident i = payment();
        workflow.adminAdvance(user("A001"), i.getPublicId(), "IN_PROGRESS");
        assertEquals(IncidentStatus.IN_PROGRESS, i.getStatus());
        assertThrows(InvalidStateTransitionException.class, () -> workflow.adminAdvance(user("A001"), i.getPublicId(), "CLOSED"));
    }

    @Test void manualReassignmentRespectsWorkload() {
        Incident i = payment();
        String other = i.getTechnicianId().equals("T201") ? "T302" : "T201";
        workflow.reassign(user("A001"), i.getPublicId(), other);
        assertEquals(other, i.getTechnicianId());
        assertThrows(ResourceNotFoundException.class, () -> workflow.reassign(user("A001"), i.getPublicId(), "T999"));
    }

    @Test void recommendationsShowEveryScoreComponent() {
        Incident i = payment();
        var ranked = workflow.recommendations(user("A001"), i.getPublicId());
        assertFalse(ranked.isEmpty());
        var top = ranked.get(0);
        assertTrue(top.eligible());
        assertEquals(top.score(), Math.round((0.4 * top.skillMatch() + 0.3 * top.loadScore() + 0.2 * top.experienceScore()
                + 0.1 * top.historyScore()) * 1000) / 1000.0, 0.002);
    }

    @Test void feedbackOncePerIncidentAfterResolution() {
        Incident i = payment();
        assertThrows(InvalidStateTransitionException.class, () -> workflow.leaveFeedback(user("E101"), i.getPublicId(), 5, "Great"));
        String tech = i.getTechnicianId();
        act(tech, i, "accept", Map.of());
        act(tech, i, "start", Map.of());
        act(tech, i, "resolve", Map.of("summary", "Restarted the gateway"));
        assertEquals(4, workflow.leaveFeedback(user("E101"), i.getPublicId(), 4, "Quick").feedbackRating());
        assertThrows(ConflictException.class, () -> workflow.leaveFeedback(user("E101"), i.getPublicId(), 5, "Again"));
        assertThrows(UnauthorizedOperationException.class, () -> workflow.leaveFeedback(user("E102"), i.getPublicId(), 5, "Not mine"));
    }

    @Test void suggestedResolutionsComeFromSimilarClosedIncidents() {
        Incident old = create("E102", "Excel crashes when opening macro workbook", "Excel crashes on open when a macro workbook is used.", null);
        String tech = old.getTechnicianId();
        act(tech, old, "accept", Map.of());
        act(tech, old, "start", Map.of());
        act(tech, old, "resolve", Map.of("summary", "Repaired the Office installation"));
        act(tech, old, "approve", Map.of());
        act("E102", old, "approve", Map.of());
        Incident fresh = create("E101", "Excel crashes when opening macro workbooks", "Excel crashes on open whenever a macro workbook is used.", null);
        var suggestions = workflow.suggestions(user("A001"), fresh.getPublicId());
        assertEquals(old.getPublicId(), suggestions.get(0).publicId());
    }

    @Test void t25AuditRepositoryHasNoUpdateOrDeletePath() {
        for (var m : com.resolveit.repository.AuditEventRepository.class.getMethods()) {
            assertFalse(m.getName().startsWith("delete") || m.getName().startsWith("update"), m.getName());
        }
        assertFalse(org.springframework.data.repository.CrudRepository.class.isAssignableFrom(com.resolveit.repository.AuditEventRepository.class));
    }
}
