package com.resolveit.web.dto;


/** Create or update an IT service. */
public record ServiceRequest(String name, String description, String category, Integer criticality, String keywords, Boolean active) { }
