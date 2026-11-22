package com.resolveit.domain.sla;

/** State of one SLA clock (response or resolution). */
public enum SlaClockState {
    ON_TRACK, APPROACHING, VIOLATED, MET
}
