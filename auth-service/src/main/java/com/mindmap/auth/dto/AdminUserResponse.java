package com.mindmap.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed user representation visible exclusively to administrators.
 */
public record AdminUserResponse(
    UUID id,
    String userId,
    String displayName,
    List<String> roles,
    boolean isActive,
    boolean isAdminSeeded,
    boolean mustChangePassword,
    int failedLoginAttempts,
    Instant lockedUntil,
    Instant createdAt,
    Instant updatedAt
) {}
