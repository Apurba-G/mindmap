package com.mindmap.assessment.dto;

import com.mindmap.assessment.entity.AssessmentLevel;

import java.util.Map;
import java.util.UUID;

/**
 * In-memory representation of session state stored in Redis hash.
 */
public record SessionData(
        UUID sessionId,
        UUID userId,
        AssessmentLevel level,
        int answeredCount,
        int totalQuestions,
        UUID currentQuestionId,
        long startTimeEpochMs,
        Map<String, String> answers
) {}
