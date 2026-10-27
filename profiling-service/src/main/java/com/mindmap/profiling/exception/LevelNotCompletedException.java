package com.mindmap.profiling.exception;

import com.mindmap.profiling.entity.ProfileLevel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an operation expects a particular assessment level to be completed,
 * but that level has not yet been processed for the user.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LevelNotCompletedException extends RuntimeException {

    public LevelNotCompletedException(String message) {
        super(message);
    }

    public LevelNotCompletedException(UUID userId, ProfileLevel level) {
        super("Assessment level " + level + " has not been completed by user: " + userId);
    }
}
