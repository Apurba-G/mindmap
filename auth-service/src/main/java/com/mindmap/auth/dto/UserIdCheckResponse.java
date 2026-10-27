package com.mindmap.auth.dto;

/**
 * Response payload for User ID availability check.
 */
public record UserIdCheckResponse(
    String userId,
    boolean available,
    String checkedVia
) {}
