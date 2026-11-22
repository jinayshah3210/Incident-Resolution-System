package com.resolveit.web.dto;


/** Admin edit of an ongoing incident; priority and SLA are recomputed. */
public record AdminIncidentUpdate(String title, String description, Integer impact, Integer urgency, Integer affectedUsers, String category) { }
