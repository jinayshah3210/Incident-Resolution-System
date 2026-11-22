package com.resolveit.strategy.priority;

import com.resolveit.exception.ValidationException;

/** The four 1-4 scales every priority strategy receives. */
public record PriorityInput(int impact, int urgency, int criticality, int affectedUsers) {
    public PriorityInput {
        check(impact, "impact");
        check(urgency, "urgency");
        check(criticality, "criticality");
        check(affectedUsers, "affectedUsers");
    }

    private static void check(int value, String field) {
        if (value < 1 || value > 4) throw ValidationException.field(field, "Values must be between 1 and 4.");
    }
}
