package com.resolveit.web.dto;


/** Help-desk contact details. */
public record SupportContactDto(String email, String phone, String hours, String message) { }
