package com.mindmap.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when recovery paragraph verification fails during account recovery.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class RecoveryFailedException extends RuntimeException {

    public RecoveryFailedException(String message) {
        super(message);
    }
}
