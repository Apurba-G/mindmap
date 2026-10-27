package com.mindmap.assessment.controller;

import com.mindmap.assessment.dto.CreateSessionRequest;
import com.mindmap.assessment.dto.SessionResponse;
import com.mindmap.assessment.security.SecurityUtils;
import com.mindmap.assessment.service.CompletionService;
import com.mindmap.assessment.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for managing assessment sessions (create, resume, complete, abandon).
 */
@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final CompletionService completionService;

    /**
     * Creates a new assessment session for the given assessment level.
     */
    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: create session for user {} at level {}", userId, request.level());
        SessionResponse response = sessionService.createSession(userId, request.level());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Resumes the current in-progress assessment session for the authenticated user.
     */
    @GetMapping("/current")
    public ResponseEntity<SessionResponse> getCurrentSession() {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: get current in-progress session for user {}", userId);
        SessionResponse response = sessionService.resumeSession(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the details and questions for a specific session.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable("id") UUID sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: get session {} for user {}", sessionId, userId);
        SessionResponse response = sessionService.getSession(userId, sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Manually triggers completion of an assessment session.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeSession(@PathVariable("id") UUID sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: complete session {} for user {}", sessionId, userId);
        completionService.completeLevel(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Abandons an active assessment session.
     */
    @PostMapping("/{id}/abandon")
    public ResponseEntity<Void> abandonSession(@PathVariable("id") UUID sessionId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: abandon session {} for user {}", sessionId, userId);
        sessionService.abandonSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
