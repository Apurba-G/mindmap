package com.mindmap.auth.controller;

import com.mindmap.auth.dto.RecoveryInitiateRequest;
import com.mindmap.auth.dto.RecoveryInitiateResponse;
import com.mindmap.auth.dto.ResetPasswordRequest;
import com.mindmap.auth.dto.ResetPasswordResponse;
import com.mindmap.auth.exception.RecoveryFailedException;
import com.mindmap.auth.service.JwtService;
import com.mindmap.auth.service.RecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for paragraph-based account recovery and password resetting.
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Account Recovery", description = "Endpoints for initiating recovery via secret paragraph and resetting password")
public class RecoveryController {

    private final RecoveryService recoveryService;
    private final JwtService jwtService;

    @Operation(summary = "Initiate account recovery by verifying recovery paragraph")
    @PostMapping({"/recovery/initiate", "/api/v1/auth/recovery/initiate"})
    public ResponseEntity<RecoveryInitiateResponse> initiateRecovery(
            @Valid @RequestBody RecoveryInitiateRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = extractClientIp(servletRequest);
        RecoveryInitiateResponse response = recoveryService.initiateRecovery(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset password using short-lived recovery token")
    @PostMapping({"/recovery/reset", "/api/v1/auth/recovery/reset"})
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Recovery-Token", required = false) String recoveryTokenHeader
    ) {
        String token = resolveToken(authHeader, recoveryTokenHeader);
        if (!StringUtils.hasText(token)) {
            throw new RecoveryFailedException("Missing recovery token in Authorization or X-Recovery-Token header");
        }

        if (!jwtService.isRecoveryToken(token)) {
            throw new RecoveryFailedException("Invalid token type. A valid recovery token is required.");
        }

        UUID userId = jwtService.getUserIdFromToken(token);
        ResetPasswordResponse response = recoveryService.resetPassword(request, userId);
        return ResponseEntity.ok(response);
    }

    private String resolveToken(String authHeader, String recoveryTokenHeader) {
        if (StringUtils.hasText(recoveryTokenHeader)) {
            return recoveryTokenHeader.trim();
        }
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
