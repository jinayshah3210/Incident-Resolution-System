package com.resolveit.strategy.priority;

import com.resolveit.domain.incident.Priority;

/** Output of a priority strategy: the raw score, its maximum and the resulting band. */
public record PriorityResult(int score, int maxScore, Priority priority, String strategy) { }
