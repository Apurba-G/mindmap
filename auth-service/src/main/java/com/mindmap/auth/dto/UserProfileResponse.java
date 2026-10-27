package com.mindmap.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User profile view visible to the authenticated user themselves.
 */
public record UserProfileResponse(
    UUID id,
    String userId,
    String displayName,
    List<String> roles,
    boolean mustChangePassword,
    Instant createdAt
) {}
