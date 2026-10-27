package com.mindmap.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an assessment session cannot be found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("Assessment session not found with ID: " + sessionId);
    }

    public SessionNotFoundException(String message) {
        super(message);
    }
}
