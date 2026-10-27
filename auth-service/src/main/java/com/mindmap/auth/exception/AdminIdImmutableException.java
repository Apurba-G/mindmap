package com.mindmap.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when attempting to alter or delete protected seed administrator identities.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AdminIdImmutableException extends RuntimeException {

    public AdminIdImmutableException(String message) {
        super(message);
    }
}
