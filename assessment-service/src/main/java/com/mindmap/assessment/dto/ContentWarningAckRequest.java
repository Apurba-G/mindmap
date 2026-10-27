package com.mindmap.assessment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for acknowledging or consenting to content warnings.
 */
public record ContentWarningAckRequest(
        @NotNull(message = "Accepted flag is required")
        Boolean accepted
) {}
