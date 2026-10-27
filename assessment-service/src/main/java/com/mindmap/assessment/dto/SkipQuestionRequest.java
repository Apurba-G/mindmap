package com.mindmap.assessment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload to skip a skippable question.
 */
public record SkipQuestionRequest(
        @NotNull(message = "Question ID is required")
        UUID questionId
) {}
