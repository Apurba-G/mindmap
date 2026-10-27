package com.mindmap.auth.dto;

/**
 * Response payload returned after password reset.
 * Supplies a newly rotated recovery paragraph.
 */
public record ResetPasswordResponse(
    String message,
    String newRecoveryParagraph,
    String warning
) {}
