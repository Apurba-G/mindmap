package com.mindmap.assessment.dto;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentPart;
import com.mindmap.assessment.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating or updating questions by an administrator.
 */
public record AdminQuestionRequest(
        @NotNull AssessmentLevel level,
        @NotNull AssessmentPart part,
        @NotNull QuestionType questionType,
        @NotNull Integer ordinal,
        @NotBlank String questionText,
        String scenarioText,
        String contentWarning,
        boolean isSkippable,
        Integer timeLimitSec,
        UUID followUpOf,
        @NotEmpty List<AdminOptionRequest> options
) {
    public record AdminOptionRequest(
            @NotBlank String optionLabel,
            @NotBlank String optionText,
            String traitKey,
            Double scoreWeight,
            boolean isSafetyFlag
    ) {}
}
