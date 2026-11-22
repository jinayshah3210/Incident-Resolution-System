package com.resolveit.domain.notification;

/** Delivery state of an email in the outbox. SIMULATED is used while real SMTP is disabled. */
public enum EmailStatus { SIMULATED, SENT, FAILED }
