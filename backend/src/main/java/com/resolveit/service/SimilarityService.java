package com.resolveit.service;

import com.resolveit.util.TextNormalizer;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Jaccard similarity on stop-word-free, stemmed token sets (duplicate, related and suggestion features). */
@Service
public class SimilarityService {
    public static final double DUPLICATE_THRESHOLD = 0.85;
    public static final double RELATED_THRESHOLD = 0.5;
    public static final double SUGGESTION_THRESHOLD = 0.4;

    public double similarity(String a, String b) {
        Set<String> left = TextNormalizer.tokens(a);
        Set<String> right = TextNormalizer.tokens(b);
        if (left.isEmpty() && right.isEmpty()) return 0;
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return (double) intersection.size() / union.size();
    }
}
