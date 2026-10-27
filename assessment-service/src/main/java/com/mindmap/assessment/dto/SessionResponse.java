package com.mindmap.assessment.dto;

import com.mindmap.assessment.entity.AssessmentLevel;

import java.util.List;
import java.util.UUID;

/**
 * Response payload returned when creating or resuming an assessment session.
 */
public record SessionResponse(
        UUID sessionId,
        AssessmentLevel level,
        int totalQuestions,
        List<QuestionDto> questions,
        UUID resumedFrom
) {}
