package com.mindmap.assessment.dto;

import java.util.Map;

/**
 * System-wide statistics for assessment completion and safety alerts.
 */
public record AssessmentStatsResponse(
        long totalSessions,
        long inProgressSessions,
        long completedSessions,
        long abandonedSessions,
        long unreviewedSafetyFlags,
        Map<String, Long> sessionsByLevel
) {}
