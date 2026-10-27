package com.mindmap.assessment.dto;

import com.mindmap.assessment.entity.AssessmentLevel;

import java.util.Map;

/**
 * Overview of unlock and completion status for all assessment levels for the user.
 */
public record LevelStatusResponse(
        Map<AssessmentLevel, LevelStatusItem> levels,
        AssessmentLevel currentLevel
) {}
