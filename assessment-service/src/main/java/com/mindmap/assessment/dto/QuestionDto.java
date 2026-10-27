package com.mindmap.assessment.dto;

import java.util.List;
import java.util.UUID;

/**
 * Data transfer record representing a psychological question.
 */
public record QuestionDto(
        UUID id,
        Integer ordinal,
        String questionText,
        String scenarioText,
        List<OptionDto> options,
        boolean isSkippable,
        Integer timeLimitSec,
        String contentWarning
) {}
