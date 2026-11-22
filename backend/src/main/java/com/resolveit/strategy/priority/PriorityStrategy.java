package com.resolveit.strategy.priority;

import com.resolveit.strategy.NamedStrategy;

/** Strategy pattern: interchangeable algorithms that turn impact, urgency, criticality and reach into a priority. */
public interface PriorityStrategy extends NamedStrategy {
    PriorityResult compute(PriorityInput input);
}
