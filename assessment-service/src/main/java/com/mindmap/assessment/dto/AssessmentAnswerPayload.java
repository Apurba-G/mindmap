package com.mindmap.assessment.dto;

import java.util.UUID;

/**
 * Individual answer payload included in the assessment completed event.
 */
public record AssessmentAnswerPayload(
        UUID questionId,
        UUID selectedOptionId,
        String optionLabel,
        String traitKey,
        Double scoreWeight,
        boolean wasSkipped,
        Long responseTimeMs
) {}
