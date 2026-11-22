package com.resolveit.domain.incident.state;

import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.exception.InvalidStateTransitionException;

/**
 * State pattern: one implementation per lifecycle status. Every action is illegal by default;
 * a concrete state overrides only the moves that are legal from it and returns the next status.
 * Incident delegates to its current state, so the lifecycle rules live in one place.
 */
public interface IncidentState {
    IncidentStatus status();

    default IncidentStatus classify() { throw illegal("classify"); }
    default IncidentStatus offer() { throw illegal("assign"); }
    default IncidentStatus acknowledge() { throw illegal("accept"); }
    default IncidentStatus release() { throw illegal("return to the queue"); }
    default IncidentStatus start() { throw illegal("start work on"); }
    default IncidentStatus resolve() { throw illegal("resolve"); }
    default IncidentStatus verify() { throw illegal("verify"); }
    default IncidentStatus reopen() { throw illegal("reopen"); }
    default IncidentStatus close() { throw illegal("close"); }

    /** Whether the closure handshake (approve / reject closure) is open in this state. */
    default boolean acceptsClosureReview() { return false; }
    /** Whether the employee-technician conversation still accepts messages. */
    default boolean allowsConversation() { return true; }
    /** Whether the reporter may leave a satisfaction rating. */
    default boolean allowsFeedback() { return false; }

    default InvalidStateTransitionException illegal(String action) {
        return InvalidStateTransitionException.of(status(), action);
    }
}
