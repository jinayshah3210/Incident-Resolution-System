package com.resolveit.web.dto;

import java.util.Map;

/** Replacement keyword dictionary for a category. */
public record KeywordsRequest(Map<String, Integer> keywords) { }
