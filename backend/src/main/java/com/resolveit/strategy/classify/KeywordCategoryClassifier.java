package com.resolveit.strategy.classify;

import com.resolveit.domain.catalog.Category;
import com.resolveit.domain.catalog.CategoryKeyword;
import com.resolveit.util.TextNormalizer;
import java.util.Collection;
import org.springframework.stereotype.Component;

/**
 * Strategy pattern (default classifier): each category scores the summed weights of its keywords found in
 * the text; title matches count double. Ties go to the category with more distinct keyword hits.
 * No hits at all falls back to "Application" (the General IT service's category) with confidence 0.
 */
@Component
public class KeywordCategoryClassifier implements CategoryClassifier {
    public static final String KEY = "KEYWORD";
    public static final String FALLBACK_CATEGORY = "Application";

    @Override public String key() { return KEY; }
    @Override public String displayName() { return "Weighted keyword classifier"; }

    @Override
    public ClassificationResult classify(String title, String description, Collection<Category> categories) {
        String titleText = TextNormalizer.normalize(title);
        String bodyText = TextNormalizer.normalize(description);
        String best = null;
        int bestScore = 0;
        int bestHits = 0;
        int total = 0;
        for (Category category : categories) {
            if (!category.isActive()) continue;
            int score = 0;
            int hits = 0;
            for (CategoryKeyword keyword : category.getKeywords()) {
                int found = 2 * TextNormalizer.countPhrase(titleText, keyword.getKeyword())
                        + TextNormalizer.countPhrase(bodyText, keyword.getKeyword());
                if (found > 0) {
                    hits++;
                    score += found * keyword.getWeight();
                }
            }
            total += score;
            boolean better = score > bestScore || (score == bestScore && score > 0 && hits > bestHits);
            if (better) {
                best = category.getName();
                bestScore = score;
                bestHits = hits;
            }
        }
        if (best == null) return new ClassificationResult(FALLBACK_CATEGORY, 0, 0);
        int confidence = (int) Math.round(100.0 * bestScore / Math.max(1, total));
        return new ClassificationResult(best, confidence, bestScore);
    }
}
