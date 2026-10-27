package com.mindmap.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when attempting to register a username that is already claimed or reserved.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserIdTakenException extends RuntimeException {

    public UserIdTakenException(String message) {
        super(message);
    }

    public UserIdTakenException(String userId, String reason) {
        super(String.format("User ID '%s' is unavailable: %s", userId, reason));
    }
}
