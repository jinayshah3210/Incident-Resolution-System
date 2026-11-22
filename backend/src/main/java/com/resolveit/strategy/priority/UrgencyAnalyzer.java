package com.resolveit.strategy.priority;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Derives urgency (how soon the business needs it fixed) from the incident text. */
@Component
public class UrgencyAnalyzer extends KeywordScaleAnalyzer {
    @Override
    protected Map<Integer, List<String>> defineRules() {
        return Map.of(
                4, List.of("urgent", "immediately", "asap", "critical", "production", "unable to complete", "blocking",
                        "deadline today", "emergency"),
                3, List.of("cannot work", "can't work", "stuck", "important", "today", "blocked"),
                2, List.of("soon", "tomorrow", "delay", "this week"));
    }
}
