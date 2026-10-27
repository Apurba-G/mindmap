package com.mindmap.profiling.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a specified scoring rule cannot be located.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ScoringRuleNotFoundException extends RuntimeException {

    public ScoringRuleNotFoundException(String message) {
        super(message);
    }

    public ScoringRuleNotFoundException(UUID ruleId) {
        super("Scoring rule not found with ID: " + ruleId);
    }
}
