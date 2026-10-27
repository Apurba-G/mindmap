package com.mindmap.auth.controller;

import com.mindmap.auth.dto.LoginRequest;
import com.mindmap.auth.dto.LoginResponse;
import com.mindmap.auth.dto.RegisterRequest;
import com.mindmap.auth.dto.RegisterResponse;
import com.mindmap.auth.service.AuthenticationService;
import com.mindmap.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller exposing user registration, login, and logout endpoints.
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, and session termination")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    @Operation(summary = "Register a new user account")
    @PostMapping({"/register", "/api/v1/auth/register"})
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = extractClientIp(servletRequest);
        RegisterResponse response = registrationService.register(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Authenticate user credentials and obtain JWT access token")
    @PostMapping({"/login", "/api/v1/auth/login"})
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = extractClientIp(servletRequest);
        LoginResponse response = authenticationService.login(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout user (stateless token invalidation confirmation)")
    @PostMapping({"/logout", "/api/v1/auth/logout"})
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
