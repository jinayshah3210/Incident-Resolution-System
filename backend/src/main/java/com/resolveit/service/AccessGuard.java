package com.resolveit.service;

import com.resolveit.domain.incident.Incident;
import com.resolveit.domain.user.PortalUser;
import com.resolveit.domain.user.Role;
import com.resolveit.exception.UnauthorizedOperationException;
import java.util.Arrays;

/** Central role and ownership checks used by every service (controllers never decide access). */
public final class AccessGuard {
    private AccessGuard() { }

    public static void requireRole(PortalUser actor, Role... allowed) {
        if (Arrays.stream(allowed).noneMatch(actor::hasRole)) {
            String label = allowed.length == 1 ? allowed[0].getLabel() : "A different";
            throw new UnauthorizedOperationException(label + " access required.");
        }
    }

    public static void requireAdmin(PortalUser actor) { requireRole(actor, Role.ADMIN); }

    /** Ownership check (prevents IDOR): delegates to the polymorphic PortalUser.canView. */
    public static void requireView(PortalUser actor, Incident incident) {
        if (!actor.canView(incident)) throw new UnauthorizedOperationException("You do not have access to this incident.");
    }
}
