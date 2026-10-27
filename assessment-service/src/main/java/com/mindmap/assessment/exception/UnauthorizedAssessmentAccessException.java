package com.mindmap.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts to access an assessment session belonging to another user.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAssessmentAccessException extends RuntimeException {

    public UnauthorizedAssessmentAccessException(String message) {
        super(message);
    }
}
