package com.mindmap.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a password does not meet the platform complexity rules.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
