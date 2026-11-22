package com.resolveit.web.dto;

import java.time.Instant;

/** One incident note. */
public record NoteDto(Long id, String type, String authorId, String authorName, String content, Instant createdAt) { }
