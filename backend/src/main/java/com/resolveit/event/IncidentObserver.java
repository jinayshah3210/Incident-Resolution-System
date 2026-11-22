package com.resolveit.event;

/** Observer pattern: reacts to incident events (audit, notifications, SLA bookkeeping). */
public interface IncidentObserver {
    void onEvent(IncidentEvent event);

    /** Lower values run first. */
    default int order() { return 100; }
}
