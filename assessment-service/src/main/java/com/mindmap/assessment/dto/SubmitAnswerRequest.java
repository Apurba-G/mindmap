package com.mindmap.assessment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload when submitting an answer for a question.
 */
public record SubmitAnswerRequest(
        @NotNull(message = "Question ID is required")
        UUID questionId,

        UUID selectedOptionId,

        Integer responseTimeMs
) {}
