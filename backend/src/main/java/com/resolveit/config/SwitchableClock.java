package com.resolveit.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Application clock. Normally the system UTC clock truncated to microseconds (the PostgreSQL precision);
 * the demo seeder and tests can pin it to an explicit instant so timestamps are deterministic.
 */
public class SwitchableClock extends Clock {
    private final Clock system = Clock.systemUTC();
    private volatile Instant fixed;

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }

    @Override
    public Instant instant() {
        Instant pinned = fixed;
        return pinned != null ? pinned : system.instant().truncatedTo(ChronoUnit.MICROS);
    }

    public void setInstant(Instant instant) { this.fixed = instant.truncatedTo(ChronoUnit.MICROS); }

    public void advance(java.time.Duration duration) { setInstant(instant().plus(duration)); }

    public void reset() { this.fixed = null; }

    public boolean isPinned() { return fixed != null; }
}
