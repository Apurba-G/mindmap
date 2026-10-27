package com.mindmap.auth.controller;

import com.mindmap.auth.dto.ChangePasswordRequest;
import com.mindmap.auth.dto.UserProfileResponse;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.exception.ResourceNotFoundException;
import com.mindmap.auth.repository.UserRepository;
import com.mindmap.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

/**
 * Controller providing self-service profile and password management for authenticated users.
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "User Self-Service", description = "Endpoints for authenticated user profile viewing and credentials update")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Operation(summary = "Get current authenticated user profile")
    @GetMapping({"/me", "/api/v1/auth/me"})
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveAuthenticatedUser(userDetails);
        UserProfileResponse response = userService.getProfile(user.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change password for the current authenticated user")
    @PostMapping({"/me/change-password", "/api/v1/auth/me/change-password"})
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        User user = resolveAuthenticatedUser(userDetails);
        String clientIp = extractClientIp(servletRequest);
        userService.changePassword(user.getId(), request, clientIp);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private User resolveAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("No authenticated principal found");
        }
        String lowerUsername = userDetails.getUsername().toLowerCase(Locale.ROOT);
        return userRepository.findByUserIdLower(lowerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
