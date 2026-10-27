package com.mindmap.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a supplied username does not conform to required format or constraints.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserIdInvalidException extends RuntimeException {

    public UserIdInvalidException(String message) {
        super(message);
    }
}
