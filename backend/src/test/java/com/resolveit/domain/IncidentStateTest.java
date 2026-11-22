package com.resolveit.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.TestData;
import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.sla.SlaClockState;
import com.resolveit.exception.InvalidStateTransitionException;
import com.resolveit.exception.ValidationException;
import com.resolveit.factory.IncidentStateFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** State pattern: every legal transition works and every illegal one throws InvalidStateTransitionException. */
class IncidentStateTest {
    private static final Map<IncidentStatus, Set<IncidentStatus>> LEGAL = Map.of(
            IncidentStatus.NEW, EnumSet.of(IncidentStatus.CLASSIFIED),
            IncidentStatus.CLASSIFIED, EnumSet.of(IncidentStatus.ASSIGNED),
            IncidentStatus.ASSIGNED, EnumSet.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.CLASSIFIED),
            IncidentStatus.ACKNOWLEDGED, EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.CLASSIFIED),
            IncidentStatus.IN_PROGRESS, EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.CLASSIFIED),
            IncidentStatus.RESOLVED, EnumSet.of(IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS),
            IncidentStatus.VERIFIED, EnumSet.of(IncidentStatus.CLOSED),
            IncidentStatus.CLOSED, EnumSet.noneOf(IncidentStatus.class));

    static Stream<Arguments> legalMoves() {
        return LEGAL.entrySet().stream().flatMap(e -> e.getValue().stream().map(to -> Arguments.of(e.getKey(), to)));
    }

    static Stream<Arguments> illegalMoves() {
        return Arrays.stream(IncidentStatus.values()).flatMap(from -> Arrays.stream(IncidentStatus.values())
                .filter(to -> !LEGAL.get(from).contains(to)).map(to -> Arguments.of(from, to)));
    }

    private static Incident at(IncidentStatus status) {
        if (status == IncidentStatus.VERIFIED) {
            // VERIFIED is transient in normal flow (it closes immediately); reach it through the generic move.
            Incident i = TestData.incidentIn(IncidentStatus.RESOLVED);
            i.transitionTo(IncidentStatus.VERIFIED, TestData.T0.plusSeconds(55));
            return i;
        }
        return TestData.incidentIn(status);
    }

    @ParameterizedTest(name = "{0} -> {1} is legal")
    @MethodSource("legalMoves")
    void legalTransitionsSucceed(IncidentStatus from, IncidentStatus to) {
        Incident incident = at(from);
        if (to == IncidentStatus.ASSIGNED) incident.offerTo(TestData.TECH, TestData.T0.plusSeconds(100));
        else if (to == IncidentStatus.RESOLVED) incident.resolve("Restarted the gateway", "", "", TestData.T0.plusSeconds(100));
        else incident.transitionTo(to, TestData.T0.plusSeconds(100));
        assertEquals(to, incident.getStatus());
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @MethodSource("illegalMoves")
    void illegalTransitionsThrow(IncidentStatus from, IncidentStatus to) {
        Incident incident = at(from);
        assertThrows(InvalidStateTransitionException.class, () -> incident.transitionTo(to, TestData.T0.plusSeconds(100)));
        assertEquals(from, incident.getStatus(), "status must not change after a rejected move");
    }

    @Test void t08NewCannotJumpToClosed() {
        InvalidStateTransitionException e = assertThrows(InvalidStateTransitionException.class,
                () -> TestData.newIncident().transitionTo(IncidentStatus.CLOSED, TestData.T0));
        assertEquals("NEW", e.getFromState());
        assertEquals("close", e.getAttemptedAction());
    }

    @Test void t09AssignedCannotResolve() {
        Incident i = TestData.incidentIn(IncidentStatus.ASSIGNED);
        assertThrows(InvalidStateTransitionException.class, () -> i.resolve("Fixed it now", "", "", TestData.T0));
    }

    @Test void fullLifecycleSetsEveryTimestamp() {
        Incident i = TestData.incidentIn(IncidentStatus.CLOSED);
        assertEquals(IncidentStatus.CLOSED, i.getStatus());
        assertNotNull(i.getClassifiedAt());
        assertNotNull(i.getAcknowledgedAt());
        assertNotNull(i.getStartedAt());
        assertNotNull(i.getResolvedAt());
        assertNotNull(i.getVerifiedAt());
        assertNotNull(i.getClosedAt());
    }

    @Test void closureNeedsBothApprovalsAndRejectionReopens() {
        Incident i = TestData.incidentIn(IncidentStatus.RESOLVED);
        assertFalse(i.approveClosure(true, TestData.T0.plusSeconds(70)));
        assertEquals(IncidentStatus.RESOLVED, i.getStatus());
        i.rejectClosure(TestData.T0.plusSeconds(80));
        assertEquals(IncidentStatus.IN_PROGRESS, i.getStatus());
        assertFalse(i.isTechnicianApproved());
        assertThrows(InvalidStateTransitionException.class, () -> i.approveClosure(false, TestData.T0.plusSeconds(90)));
    }

    @Test void declinedTechnicianIsNeverOfferedAgain() {
        Incident i = TestData.incidentIn(IncidentStatus.ASSIGNED);
        assertEquals("T201", i.decline(TestData.T0.plusSeconds(15)));
        assertEquals(IncidentStatus.CLASSIFIED, i.getStatus());
        assertNull(i.getTechnicianId());
        assertThrows(InvalidStateTransitionException.class, () -> i.offerTo(TestData.TECH, TestData.T0.plusSeconds(16)));
    }

    @Test void resolveRequiresSummary() {
        Incident i = TestData.incidentIn(IncidentStatus.IN_PROGRESS);
        assertThrows(ValidationException.class, () -> i.resolve(" ", null, null, TestData.T0));
    }

    @Test void closedConversationIsReadOnly() {
        Incident i = TestData.incidentIn(IncidentStatus.CLOSED);
        assertThrows(InvalidStateTransitionException.class, () -> i.addChatMessage(TestData.REPORTER, "hello", TestData.T0));
    }

    @Test void slaClocksAreMarkedOnceOnly() {
        Incident i = TestData.incidentIn(IncidentStatus.CLASSIFIED);
        i.applySlaDeadlines(TestData.T0.plusSeconds(300), TestData.T0.plusSeconds(1800));
        assertTrue(i.markApproaching(com.resolveit.domain.sla.ViolationType.RESPONSE));
        assertFalse(i.markApproaching(com.resolveit.domain.sla.ViolationType.RESPONSE));
        assertTrue(i.markViolated(com.resolveit.domain.sla.ViolationType.RESPONSE));
        assertFalse(i.markViolated(com.resolveit.domain.sla.ViolationType.RESPONSE));
        assertEquals(SlaClockState.VIOLATED, i.getResponseSla());
        assertTrue(i.isSlaViolated());
    }

    @ParameterizedTest
    @EnumSource(IncidentStatus.class)
    void everyStatusHasAStateObject(IncidentStatus status) {
        assertEquals(status, IncidentStateFactory.forStatus(status).status());
    }

    @Test void incidentHasNoPublicStatusSetter() {
        for (Method m : Incident.class.getDeclaredMethods()) {
            boolean setter = m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers());
            assertFalse(setter, "Incident must not expose public setter " + m.getName());
        }
    }
}
