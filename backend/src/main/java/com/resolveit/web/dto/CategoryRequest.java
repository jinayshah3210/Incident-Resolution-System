package com.resolveit.web.dto;


/** Create or update a category. */
public record CategoryRequest(String name, String description, Boolean active) { }
