package com.resolveit.strategy.priority;

import com.resolveit.domain.incident.Priority;
import org.springframework.stereotype.Component;

/**
 * Strategy pattern (default): score = max(impact, affectedUsers) x urgency x criticality, range 1-64.
 * The blast radius is the larger of how broken the service is and how many people are affected.
 * P1 >= 36, P2 18-35, P3 8-17, P4 <= 7.
 */
@Component
public class MultiplicativePriorityStrategy implements PriorityStrategy {
    public static final String KEY = "MULTIPLICATIVE";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Impact x urgency x criticality"; }

    @Override
    public PriorityResult compute(PriorityInput input) {
        int reach = Math.max(input.impact(), input.affectedUsers());
        int score = reach * input.urgency() * input.criticality();
        return new PriorityResult(score, 64, band(score), KEY);
    }

    static Priority band(int score) {
        if (score >= 36) return Priority.P1;
        if (score >= 18) return Priority.P2;
        if (score >= 8) return Priority.P3;
        return Priority.P4;
    }
}
