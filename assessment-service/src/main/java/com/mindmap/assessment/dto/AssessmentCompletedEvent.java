package com.mindmap.assessment.dto;

import com.mindmap.assessment.entity.AssessmentLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Kafka event dispatched to 'assessment.completed' topic upon assessment level completion.
 */
public record AssessmentCompletedEvent(
        UUID userId,
        AssessmentLevel level,
        UUID sessionId,
        List<AssessmentAnswerPayload> answers,
        LocalDateTime completedAt,
        Integer totalTimeSec
) {}
