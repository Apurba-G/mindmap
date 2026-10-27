package com.mindmap.auth.controller;

import com.mindmap.auth.dto.AdminUserResponse;
import com.mindmap.auth.entity.AuditLogEntry;
import com.mindmap.auth.entity.User;
import com.mindmap.auth.exception.ResourceNotFoundException;
import com.mindmap.auth.repository.AuditLogRepository;
import com.mindmap.auth.repository.UserRepository;
import com.mindmap.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

/**
 * Controller exposing administrative management operations.
 * Requires ROLE_ADMIN privilege.
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Administration", description = "Endpoints for administrators to manage users and inspect audit logs")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Operation(summary = "List all user accounts with pagination")
    @GetMapping({"/admin/users", "/api/v1/auth/admin/users"})
    public ResponseEntity<Page<AdminUserResponse>> listUsers(@PageableDefault(size = 20) Pageable pageable) {
        Page<AdminUserResponse> page = userService.getAllUsers(pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Get user account details by ID")
    @GetMapping({"/admin/users/{id}", "/api/v1/auth/admin/users/{id}"})
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable("id") UUID id) {
        AdminUserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Activate or deactivate a user account")
    @PatchMapping({"/admin/users/{id}/status", "/api/v1/auth/admin/users/{id}/status"})
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable("id") UUID id,
            @RequestParam("active") boolean active,
            @AuthenticationPrincipal UserDetails adminPrincipal,
            HttpServletRequest servletRequest
    ) {
        User admin = resolveAdmin(adminPrincipal);
        String clientIp = extractClientIp(servletRequest);
        AdminUserResponse response = userService.updateUserStatus(id, active, admin.getId(), clientIp);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Unlock a locked user account")
    @PostMapping({"/admin/users/{id}/unlock", "/api/v1/auth/admin/users/{id}/unlock"})
    public ResponseEntity<AdminUserResponse> unlockUser(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserDetails adminPrincipal,
            HttpServletRequest servletRequest
    ) {
        User admin = resolveAdmin(adminPrincipal);
        String clientIp = extractClientIp(servletRequest);
        AdminUserResponse response = userService.unlockUser(id, admin.getId(), clientIp);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search and view audit logs")
    @GetMapping({"/admin/audit-logs", "/api/v1/auth/admin/audit-logs"})
    public ResponseEntity<Page<AuditLogEntry>> getAuditLogs(
            @RequestParam(value = "actorId", required = false) UUID actorId,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "resourceId", required = false) String resourceId,
            @PageableDefault(size = 25) Pageable pageable
    ) {
        if (actorId != null) {
            return ResponseEntity.ok(auditLogRepository.findByActorId(actorId, pageable));
        }
        if (resourceType != null && resourceId != null) {
            return ResponseEntity.ok(auditLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId, pageable));
        }
        return ResponseEntity.ok(auditLogRepository.findAll(pageable));
    }

    private User resolveAdmin(UserDetails adminPrincipal) {
        if (adminPrincipal == null) {
            throw new ResourceNotFoundException("No authenticated admin principal found");
        }
        String lowerUsername = adminPrincipal.getUsername().toLowerCase(Locale.ROOT);
        return userRepository.findByUserIdLower(lowerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin account not found in database"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
