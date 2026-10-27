package com.mindmap.assessment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an attempt is made to skip a non-skippable question.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuestionNotSkippableException extends RuntimeException {

    public QuestionNotSkippableException(UUID questionId) {
        super("Question is mandatory and cannot be skipped: " + questionId);
    }
}
