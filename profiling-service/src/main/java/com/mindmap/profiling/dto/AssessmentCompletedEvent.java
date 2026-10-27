package com.mindmap.profiling.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka event consumed from topic 'assessment.completed'.
 *
 * @param userId      the user UUID who completed the assessment
 * @param level       the level completed (e.g. "LEVEL_1", "LEVEL_2", "LEVEL_3")
 * @param sessionId   the assessment session UUID
 * @param answers     the list of answer submissions
 * @param completedAt timestamp when the assessment was finalized
 */
public record AssessmentCompletedEvent(
        @NotNull UUID userId,
        @NotNull String level,
        @NotNull UUID sessionId,
        @NotEmpty List<AnswerData> answers,
        @NotNull Instant completedAt
) {
}
