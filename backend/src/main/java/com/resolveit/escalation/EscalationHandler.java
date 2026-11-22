package com.resolveit.escalation;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.PortalUser;
import java.util.List;

/**
 * Chain of Responsibility: each handler owns one escalation level. A request passes along the chain;
 * a handler acts when the request reaches its level and the incident has not been escalated that far yet.
 * New levels are added by linking another handler, without changing callers.
 */
public abstract class EscalationHandler {
    private final EscalationRecorder recorder;
    private EscalationHandler next;

    protected EscalationHandler(EscalationRecorder recorder) { this.recorder = recorder; }

    /** Links the next handler and returns it, so chains read technician.linkWith(lead).linkWith(admin). */
    public EscalationHandler linkWith(EscalationHandler nextHandler) {
        this.next = nextHandler;
        return nextHandler;
    }

    public final void handle(EscalationRequest request) {
        if (request.targetLevel() >= level() && request.incident().getEscalationLevel() < level()) {
            recorder.record(request, level(), targetRole(), recipients(request.incident()));
        }
        if (next != null) next.handle(request);
    }

    public abstract int level();

    public abstract String targetRole();

    protected abstract List<? extends PortalUser> recipients(Incident incident);
}
