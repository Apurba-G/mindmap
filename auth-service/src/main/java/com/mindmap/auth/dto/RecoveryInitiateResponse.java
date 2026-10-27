package com.mindmap.auth.dto;

/**
 * Response payload returned after verifying recovery paragraph.
 * Supplies a short-lived recovery token used exclusively to reset the password.
 */
public record RecoveryInitiateResponse(
    String recoveryToken,
    long expiresIn
) {}
