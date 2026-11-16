package com.irs.backend.state;

import com.irs.backend.model.Incident;
import com.irs.backend.model.enums.IncidentStatus;
import com.irs.backend.exception.InvalidIncidentStateException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentStateManagerTest {

    private final IncidentStateManager manager =
            new IncidentStateManager();

    private Incident createIncident() {
        return new Incident(
                "Server Down",
                "Server is not responding",
                "NETWORK"
        );
    }

    @Test
    void testNewToAssigned() {
        Incident incident = createIncident();

        manager.assign(incident);

        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());
    }

    @Test
    void testAssignedToInProgress() {
        Incident incident = createIncident();

        manager.assign(incident);
        manager.startProgress(incident);

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
    }

    @Test
    void testInProgressToOnHold() {
        Incident incident = createIncident();

        manager.assign(incident);
        manager.startProgress(incident);
        manager.putOnHold(incident);

        assertEquals(IncidentStatus.ON_HOLD, incident.getStatus());
    }

    @Test
    void testOnHoldToInProgress() {
        Incident incident = createIncident();

        manager.assign(incident);
        manager.startProgress(incident);
        manager.putOnHold(incident);
        manager.startProgress(incident);

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
    }

    @Test
    void testInProgressToResolved() {
        Incident incident = createIncident();

        manager.assign(incident);
        manager.startProgress(incident);
        manager.resolve(incident);

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    @Test
    void testResolvedToClosed() {
        Incident incident = createIncident();

        manager.assign(incident);
        manager.startProgress(incident);
        manager.resolve(incident);
        manager.close(incident);

        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
    }

    @Test
    void testResolvedToReopened() {
        Incident incident = createIncident();

        manager.assign(incident);
        manager.startProgress(incident);
        manager.resolve(incident);
        manager.reopen(incident);

        assertEquals(IncidentStatus.REOPENED, incident.getStatus());
    }

    @Test
    void testInvalidTransition() {
        Incident incident = createIncident();

        assertThrows(
                InvalidIncidentStateException.class,
                () -> manager.close(incident)
        );
    }
}