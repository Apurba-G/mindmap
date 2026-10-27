package com.mindmap.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller for when downstream services are unavailable.
 * Spring Cloud Gateway routes can be configured with CircuitBreaker
 * filters that redirect to these endpoints on failure.
 */
@Slf4j
@RestController
public class FallbackController {

    @GetMapping("/fallback/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.error("Auth Service is unavailable — circuit breaker triggered");
        return buildFallbackResponse("AUTH_SERVICE_UNAVAILABLE",
            "The authentication service is temporarily unavailable. Please try again shortly.");
    }

    @GetMapping("/fallback/assessment")
    public ResponseEntity<Map<String, Object>> assessmentFallback() {
        log.error("Assessment Service is unavailable — circuit breaker triggered");
        return buildFallbackResponse("ASSESSMENT_SERVICE_UNAVAILABLE",
            "The assessment service is temporarily unavailable. Your progress is saved and you can resume later.");
    }

    @GetMapping("/fallback/profiling")
    public ResponseEntity<Map<String, Object>> profilingFallback() {
        log.error("Profiling Service is unavailable — circuit breaker triggered");
        return buildFallbackResponse("PROFILING_SERVICE_UNAVAILABLE",
            "The profiling service is temporarily unavailable. Please try again shortly.");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String error, String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "error", error,
                "message", message,
                "timestamp", Instant.now().toString(),
                "retryAfterSeconds", 30
            ));
    }
}
