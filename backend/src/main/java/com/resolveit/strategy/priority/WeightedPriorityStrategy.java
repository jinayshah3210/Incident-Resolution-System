package com.resolveit.strategy.priority;

import com.resolveit.domain.incident.Priority;
import org.springframework.stereotype.Component;

/**
 * Strategy pattern: weighted percentage score
 * score = (0.35 x impact + 0.25 x urgency + 0.25 x criticality + 0.15 x affectedUsers) / 4 x 100, range 25-100.
 * P1 >= 85, P2 >= 65, P3 >= 40, otherwise P4.
 */
@Component
public class WeightedPriorityStrategy implements PriorityStrategy {
    public static final String KEY = "WEIGHTED";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Weighted score"; }

    @Override
    public PriorityResult compute(PriorityInput input) {
        double weighted = 0.35 * input.impact() + 0.25 * input.urgency() + 0.25 * input.criticality()
                + 0.15 * input.affectedUsers();
        int score = (int) Math.round(weighted / 4.0 * 100.0);
        return new PriorityResult(score, 100, band(score), KEY);
    }

    static Priority band(int score) {
        if (score >= 85) return Priority.P1;
        if (score >= 65) return Priority.P2;
        if (score >= 40) return Priority.P3;
        return Priority.P4;
    }
}
