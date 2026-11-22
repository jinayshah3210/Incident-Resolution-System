package com.resolveit.strategy.classify;

/** Category chosen by a classifier with a 0-100 confidence. */
public record ClassificationResult(String category, int confidence, int score) { }
