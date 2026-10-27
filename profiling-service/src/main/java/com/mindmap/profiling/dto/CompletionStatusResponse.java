package com.mindmap.profiling.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Assessment completion status response for a user.
 *
 * @param userId            the user UUID
 * @param isComplete        whether all levels are completed
 * @param highestLevel      the highest level completed so far
 * @param overallConfidence the confidence score (0-100)
 * @param completedLevels   list of completed level codes
 * @param updatedAt         timestamp of the last profile update
 */
public record CompletionStatusResponse(
        UUID userId,
        boolean isComplete,
        String highestLevel,
        Double overallConfidence,
        List<String> completedLevels,
        Instant updatedAt
) {
}
