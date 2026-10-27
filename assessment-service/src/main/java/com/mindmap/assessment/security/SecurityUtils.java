package com.mindmap.assessment.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility helper methods to retrieve authentication context and user identifiers.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Retrieves the authenticated UserPrincipal from the SecurityContext.
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Retrieves the current authenticated user's UUID.
     *
     * @throws IllegalStateException if not authenticated
     */
    public static UUID getCurrentUserId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::id)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in SecurityContext"));
    }
}
