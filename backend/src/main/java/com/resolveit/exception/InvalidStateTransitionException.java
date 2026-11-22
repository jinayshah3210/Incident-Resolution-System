package com.resolveit.exception;

import org.springframework.http.HttpStatus;

/** HTTP 409: an illegal incident lifecycle move, carrying the current state and the attempted action. */
public class InvalidStateTransitionException extends ItsmException {
    private final String fromState;
    private final String attemptedAction;

    public InvalidStateTransitionException(String message) {
        this(null, null, message);
    }

    public InvalidStateTransitionException(String fromState, String attemptedAction, String message) {
        super("INVALID_STATE_TRANSITION", HttpStatus.CONFLICT, message);
        this.fromState = fromState;
        this.attemptedAction = attemptedAction;
    }

    public static InvalidStateTransitionException of(Enum<?> from, String action) {
        String readable = from.name().replace('_', ' ').toLowerCase();
        return new InvalidStateTransitionException(from.name(), action,
                "Cannot " + action + " an incident that is " + readable + ".");
    }

    public String getFromState() { return fromState; }
    public String getAttemptedAction() { return attemptedAction; }
}
