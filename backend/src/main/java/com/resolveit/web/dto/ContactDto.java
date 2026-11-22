package com.resolveit.web.dto;


/** Someone the user can talk to in an incident conversation. */
public record ContactDto(String id, String name, String role, String incidentId) { }
