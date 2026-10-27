package com.mindmap.profiling.controller;

import com.mindmap.profiling.dto.FullProfileResponse;
import com.mindmap.profiling.dto.LevelScoresResponse;
import com.mindmap.profiling.dto.TemperamentResponse;
import com.mindmap.profiling.entity.ProfileAccessAudit;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.security.UserPrincipal;
import com.mindmap.profiling.service.ProfileAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Administrative controller for inspecting detailed diagnostic psychological profiles and audit trails.
 * Access is restricted to users with the ROLE_ADMIN authority and all reads are strictly audited.
 */
@Slf4j
@RestController
@RequestMapping({"/admin", "/api/v1/admin/profiles"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProfileController {

    private final ProfileAccessService profileAccessService;

    /**
     * Retrieves the complete diagnostic profile for a target user.
     *
     * @param id        target user ID
     * @param principal authenticated administrator principal
     * @param request   HTTP servlet request for client IP extraction
     * @return full diagnostic profile response
     */
    @GetMapping("/users/{id}/full")
    public ResponseEntity<FullProfileResponse> getFullProfile(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        String clientIp = resolveClientIp(request);
        FullProfileResponse response = profileAccessService.getFullProfile(
                id,
                principal.getUserId(),
                principal.getHandle(),
                clientIp
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves scores for a specific assessment level for a target user.
     *
     * @param id        target user ID
     * @param level     assessment level string
     * @param principal authenticated administrator principal
     * @param request   HTTP servlet request
     * @return level scores response
     */
    @GetMapping({"/users/{id}/scores/{level}", "/scores/{level}"})
    public ResponseEntity<LevelScoresResponse> getLevelScores(
            @PathVariable(value = "id", required = false) UUID id,
            @PathVariable("level") String level,
            @RequestParam(value = "userId", required = false) UUID queryUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        UUID targetUserId = id != null ? id : queryUserId;
        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user ID must be provided in path or query parameters");
        }

        ProfileLevel profileLevel = ProfileLevel.fromString(level);
        String clientIp = resolveClientIp(request);

        LevelScoresResponse response = profileAccessService.getLevelScores(
                targetUserId,
                profileLevel,
                principal.getUserId(),
                principal.getHandle(),
                clientIp
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the temperament profile breakdown for a target user.
     *
     * @param id        target user ID
     * @param principal authenticated administrator principal
     * @param request   HTTP servlet request
     * @return temperament response
     */
    @GetMapping({"/users/{id}/temperament", "/temperament"})
    public ResponseEntity<TemperamentResponse> getTemperament(
            @PathVariable(value = "id", required = false) UUID id,
            @RequestParam(value = "userId", required = false) UUID queryUserId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        UUID targetUserId = id != null ? id : queryUserId;
        if (targetUserId == null) {
            throw new IllegalArgumentException("Target user ID must be provided in path or query parameters");
        }

        String clientIp = resolveClientIp(request);
        TemperamentResponse response = profileAccessService.getTemperament(
                targetUserId,
                principal.getUserId(),
                principal.getHandle(),
                clientIp
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the paginated profile access audit trail.
     *
     * @param adminUserId  optional filter by admin ID
     * @param targetUserId optional filter by inspected user ID
     * @param pageable     pagination parameters
     * @return page of audit log entries
     */
    @GetMapping("/audit-log")
    public ResponseEntity<Page<ProfileAccessAudit>> getAuditLog(
            @RequestParam(value = "adminUserId", required = false) UUID adminUserId,
            @RequestParam(value = "targetUserId", required = false) UUID targetUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProfileAccessAudit> audits = profileAccessService.getAuditLogs(adminUserId, targetUserId, pageable);
        return ResponseEntity.ok(audits);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
