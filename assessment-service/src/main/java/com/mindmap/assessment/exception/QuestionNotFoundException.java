package com.mindmap.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a specified question cannot be found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(UUID questionId) {
        super("Question not found with ID: " + questionId);
    }

    public QuestionNotFoundException(String message) {
        super(message);
    }
}
