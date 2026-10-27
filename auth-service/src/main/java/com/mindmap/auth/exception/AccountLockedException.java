package com.mindmap.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;

/**
 * Thrown when attempting to authenticate against an account temporarily locked due to failed attempts.
 */
@ResponseStatus(HttpStatus.LOCKED)
public class AccountLockedException extends RuntimeException {

    private final Instant lockedUntil;

    public AccountLockedException(String message) {
        super(message);
        this.lockedUntil = null;
    }

    public AccountLockedException(String message, Instant lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
