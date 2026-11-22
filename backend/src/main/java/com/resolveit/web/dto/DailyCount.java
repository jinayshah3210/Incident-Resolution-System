package com.resolveit.web.dto;


/** Incidents created and closed on one day. */
public record DailyCount(String date, long created, long closed) { }
