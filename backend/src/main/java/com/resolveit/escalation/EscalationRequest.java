package com.resolveit.escalation;

import com.resolveit.domain.incident.Incident;
import java.time.Instant;

/** What travels along the chain: the incident, how far it should be escalated, and when. */
public record EscalationRequest(Incident incident, int targetLevel, long minutesOverdue, Instant at) { }
