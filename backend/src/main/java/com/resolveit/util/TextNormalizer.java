package com.resolveit.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared text utilities for classification, impact analysis and similarity detection. */
public final class TextNormalizer {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "be", "been", "being", "to", "of", "in",
            "on", "at", "for", "with", "from", "by", "as", "it", "its", "this", "that", "these", "those", "i", "we",
            "you", "he", "she", "they", "my", "our", "your", "me", "us", "them", "am", "do", "does", "did", "have",
            "has", "had", "not", "no", "so", "if", "then", "than", "there", "here", "can", "could", "would", "should",
            "will", "just", "also", "very", "please", "any", "all", "some", "when", "while", "since", "after", "before",
            "again", "still", "now", "up", "out", "into", "about", "what", "which", "who", "how", "why");

    private TextNormalizer() { }

    /** Lower-cases, strips punctuation (keeping hyphens inside words) and pads with spaces for phrase matching. */
    public static String normalize(String text) {
        if (text == null) return " ";
        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("<[^>]*>", " ").replaceAll("[^a-z0-9-]+", " ")
                .replaceAll("\\s+", " ").strip();
        return " " + cleaned + " ";
    }

    /** Counts whole-word occurrences of a word or phrase inside an already normalised text. */
    public static int countPhrase(String normalizedText, String phrase) {
        String needle = normalize(phrase);
        if (needle.isBlank()) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int index = normalizedText.indexOf(needle, from);
            if (index < 0) return count;
            count++;
            from = index + needle.length() - 1;
        }
    }

    /** Distinct, stop-word-free, lightly stemmed tokens used for Jaccard similarity. */
    public static Set<String> tokens(String text) {
        Set<String> result = new LinkedHashSet<>();
        for (String word : normalize(text).strip().split(" ")) {
            if (word.length() < 2 || STOP_WORDS.contains(word)) continue;
            result.add(stem(word));
        }
        return result;
    }

    public static List<String> words(String text) {
        List<String> result = new ArrayList<>();
        for (String word : normalize(text).strip().split(" ")) if (!word.isBlank()) result.add(word);
        return result;
    }

    /** Simple suffix-trimming stemmer: "crashing", "crashed", "crashes" all become "crash". */
    public static String stem(String word) {
        for (String suffix : List.of("ingly", "ing", "edly", "ed", "es", "s")) {
            if (word.length() > suffix.length() + 3 && word.endsWith(suffix)) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        return word;
    }

    /** Removes HTML tags and collapses whitespace so stored text is plain. */
    public static String plain(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").strip();
    }
}
