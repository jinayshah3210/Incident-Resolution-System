package com.resolveit.domain.sla;

import java.time.Duration;

/** Response and resolution targets, in policy minutes, for one priority. */
public record SlaTargets(int responseMinutes, int resolutionMinutes, boolean fallback) {
    public Duration response(int timeScale) { return scaled(responseMinutes, timeScale); }
    public Duration resolution(int timeScale) { return scaled(resolutionMinutes, timeScale); }

    private static Duration scaled(int minutes, int timeScale) {
        long seconds = Math.max(1, minutes * 60L / Math.max(1, timeScale));
        return Duration.ofSeconds(seconds);
    }
}
