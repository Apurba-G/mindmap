package com.mindmap.auth.dto;

import java.util.UUID;

/**
 * Response payload returned after successful registration.
 * Contains the generated unhashed recovery paragraph that is shown only once.
 */
public record RegisterResponse(
    UUID id,
    String userId,
    String recoveryParagraph,
    String accessToken,
    String tokenType,
    long expiresIn,
    String message
) {}
