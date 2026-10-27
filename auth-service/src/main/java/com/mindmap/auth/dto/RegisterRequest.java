package com.mindmap.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for user registration.
 */
public record RegisterRequest(
    @NotBlank(message = "User ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9._]{4,20}$", message = "User ID must be 4-20 characters consisting of letters, numbers, dots, or underscores")
    String userId,

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
    String displayName,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    String password,

    @NotBlank(message = "Password confirmation is required")
    String passwordConfirm
) {}
