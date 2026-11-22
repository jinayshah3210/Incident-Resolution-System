package com.resolveit.strategy.priority;

import com.resolveit.util.TextNormalizer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Template for the text analysers that derive a 1-4 scale from the incident text. Rules are checked
 * from the highest level down; the first level with a whole-word match wins, otherwise level 1.
 * Inheritance: ImpactAnalyzer, UrgencyAnalyzer and AffectedUsersAnalyzer are each a KeywordScaleAnalyzer.
 */
public abstract class KeywordScaleAnalyzer {
    private final TreeMap<Integer, List<String>> rules = new TreeMap<>();

    protected KeywordScaleAnalyzer() {
        rules.putAll(defineRules());
    }

    /** Level (2-4) to the phrases that indicate it. */
    protected abstract Map<Integer, List<String>> defineRules();

    public int analyze(String text) {
        String normalized = TextNormalizer.normalize(text);
        for (Map.Entry<Integer, List<String>> rule : rules.descendingMap().entrySet()) {
            for (String phrase : rule.getValue()) {
                if (TextNormalizer.countPhrase(normalized, phrase) > 0) return rule.getKey();
            }
        }
        return 1;
    }
}
