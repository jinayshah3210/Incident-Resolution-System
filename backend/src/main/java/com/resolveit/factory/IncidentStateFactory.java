package com.resolveit.factory;

import com.resolveit.domain.incident.IncidentStatus;
import com.resolveit.domain.incident.state.AcknowledgedState;
import com.resolveit.domain.incident.state.AssignedState;
import com.resolveit.domain.incident.state.ClassifiedState;
import com.resolveit.domain.incident.state.ClosedState;
import com.resolveit.domain.incident.state.InProgressState;
import com.resolveit.domain.incident.state.IncidentState;
import com.resolveit.domain.incident.state.NewState;
import com.resolveit.domain.incident.state.ResolvedState;
import com.resolveit.domain.incident.state.VerifiedState;
import java.util.EnumMap;
import java.util.Map;

/** Factory pattern: maps each stored status to its stateless State object (EnumMap of singletons). */
public final class IncidentStateFactory {
    private static final Map<IncidentStatus, IncidentState> STATES = new EnumMap<>(IncidentStatus.class);

    static {
        register(new NewState());
        register(new ClassifiedState());
        register(new AssignedState());
        register(new AcknowledgedState());
        register(new InProgressState());
        register(new ResolvedState());
        register(new VerifiedState());
        register(new ClosedState());
    }

    private IncidentStateFactory() { }

    private static void register(IncidentState state) { STATES.put(state.status(), state); }

    public static IncidentState forStatus(IncidentStatus status) {
        IncidentState state = STATES.get(status);
        if (state == null) throw new IllegalStateException("No state object registered for " + status);
        return state;
    }
}
