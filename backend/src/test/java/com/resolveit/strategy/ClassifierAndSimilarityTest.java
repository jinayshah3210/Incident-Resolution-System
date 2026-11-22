package com.resolveit.strategy;

import static org.junit.jupiter.api.Assertions.*;

import com.resolveit.domain.catalog.Category;
import com.resolveit.service.SimilarityService;
import com.resolveit.strategy.classify.ClassificationResult;
import com.resolveit.strategy.classify.KeywordCategoryClassifier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Keyword classifier and Jaccard similarity (duplicate / related / suggestion features). */
class ClassifierAndSimilarityTest {
    private final KeywordCategoryClassifier classifier = new KeywordCategoryClassifier();
    private final SimilarityService similarity = new SimilarityService();

    private static Category category(String name, Map<String, Integer> words) {
        Category c = new Category(name, name);
        c.replaceKeywords(words);
        return c;
    }

    private final List<Category> categories = List.of(
            category("Network", Map.of("wifi", 3, "vpn", 3, "network", 2)),
            category("Application", Map.of("payment", 3, "transaction", 3, "application", 2)),
            category("Hardware", Map.of("printer", 2, "laptop", 2)));

    @Test void picksHighestWeightedCategory() {
        ClassificationResult r = classifier.classify("VPN keeps dropping", "The vpn and wifi disconnect every hour", categories);
        assertEquals("Network", r.category());
        assertTrue(r.confidence() > 50);
    }

    @Test void titleMatchesCountDouble() {
        ClassificationResult r = classifier.classify("Printer jam", "Payment receipt will not print", categories);
        assertEquals("Hardware", r.category());
    }

    @Test void noKeywordFallsBackWithZeroConfidence() {
        ClassificationResult r = classifier.classify("Something odd", "It behaves strangely today", categories);
        assertEquals(KeywordCategoryClassifier.FALLBACK_CATEGORY, r.category());
        assertEquals(0, r.confidence());
    }

    @Test void inactiveCategoriesAreIgnored() {
        Category net = category("Network", Map.of("wifi", 3));
        net.deactivate();
        assertNotEquals("Network", classifier.classify("wifi down", "wifi", List.of(net)).category());
    }

    @Test void identicalTextIsFullySimilarAndStemmingHelps() {
        assertEquals(1.0, similarity.similarity("Payment portal crashed", "payment portal crashed"));
        assertEquals(1.0, similarity.similarity("printers crashing", "printer crashed"));
        assertTrue(similarity.similarity("VPN is down", "Payroll export failed") < 0.2);
        assertEquals(0.0, similarity.similarity("", ""));
    }
}
