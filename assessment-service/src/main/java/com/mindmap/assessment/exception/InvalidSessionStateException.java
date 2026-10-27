package com.mindmap.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an operation is performed on a session in an incompatible state (e.g. COMPLETED or ABANDONED).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSessionStateException extends RuntimeException {

    public InvalidSessionStateException(String message) {
        super(message);
    }
}
