package com.mindmap.auth.dto;

import java.time.Instant;

/**
 * Standard API error response record.
 */
public record ErrorResponse(
    String errorCode,
    String message,
    Object details,
    Instant timestamp
) {
    public ErrorResponse(String errorCode, String message, Object details) {
        this(errorCode, message, details, Instant.now());
    }
}
