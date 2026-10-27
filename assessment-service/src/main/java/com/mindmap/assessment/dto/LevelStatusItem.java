package com.mindmap.assessment.dto;

import com.mindmap.assessment.entity.SessionStatus;

import java.time.LocalDateTime;

/**
 * Status detail for an individual assessment level.
 */
public record LevelStatusItem(
        boolean unlocked,
        SessionStatus status,
        ProgressDto progress,
        LocalDateTime completedAt
) {}
