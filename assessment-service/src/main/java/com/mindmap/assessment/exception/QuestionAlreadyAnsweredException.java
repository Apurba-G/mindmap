package com.mindmap.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an answer is submitted for a question that has already been answered or skipped.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class QuestionAlreadyAnsweredException extends RuntimeException {

    public QuestionAlreadyAnsweredException(UUID questionId) {
        super("Question has already been answered: " + questionId);
    }

    public QuestionAlreadyAnsweredException(String message) {
        super(message);
    }
}
