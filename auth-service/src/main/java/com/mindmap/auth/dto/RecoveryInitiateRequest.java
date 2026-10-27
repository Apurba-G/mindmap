package com.mindmap.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload to initiate account recovery using the secret recovery paragraph.
 */
public record RecoveryInitiateRequest(
    @NotBlank(message = "User ID is required")
    String userId,

    @NotBlank(message = "Recovery paragraph is required")
    String recoveryParagraph
) {}
