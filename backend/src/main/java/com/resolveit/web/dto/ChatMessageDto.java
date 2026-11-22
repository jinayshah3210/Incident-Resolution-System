package com.resolveit.web.dto;

import java.time.Instant;

/** One chat message of an incident conversation. */
public record ChatMessageDto(Long id, Instant at, String senderId, String senderName, String content) { }
