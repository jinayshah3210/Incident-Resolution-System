package com.resolveit.web.dto;


/** Successful login: bearer token and the signed-in user. */
public record LoginResponse(String token, UserDto user) { }
