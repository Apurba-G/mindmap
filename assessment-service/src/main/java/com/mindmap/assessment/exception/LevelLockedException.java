package com.mindmap.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts to access or start an assessment level that is currently locked.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class LevelLockedException extends RuntimeException {

    public LevelLockedException(String message) {
        super(message);
    }
}
