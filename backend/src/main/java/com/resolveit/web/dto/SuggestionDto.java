package com.resolveit.web.dto;


/** A resolution suggested from a similar closed incident. */
public record SuggestionDto(String publicId, String title, String resolutionSummary, String rootCause, String resolutionSteps, double similarity) { }
