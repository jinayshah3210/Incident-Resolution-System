package com.resolveit.strategy.priority;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Derives impact (how badly the service is affected) from the incident text. */
@Component
public class ImpactAnalyzer extends KeywordScaleAnalyzer {
    @Override
    protected Map<Integer, List<String>> defineRules() {
        return Map.of(
                4, List.of("unavailable", "down", "outage", "all users", "everyone", "entire", "cannot access",
                        "not working for all", "unable to complete", "offline", "completely broken"),
                3, List.of("multiple users", "department", "team", "major", "failing", "transactions", "failed", "not working"),
                2, List.of("slow", "intermittent", "some users", "degraded", "delay", "sometimes", "occasionally", "lag"));
    }
}
