package com.mindmap.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for authenticated password change.
 * currentPassword may be null/empty when user is changing their initial temporary password.
 */
public record ChangePasswordRequest(
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    String newPassword,

    String newPasswordConfirm
) {}
