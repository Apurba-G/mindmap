package com.mindmap.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user authentication.
 */
public record LoginRequest(
    @NotBlank(message = "User ID is required")
    String userId,

    @NotBlank(message = "Password is required")
    String password
) {}
