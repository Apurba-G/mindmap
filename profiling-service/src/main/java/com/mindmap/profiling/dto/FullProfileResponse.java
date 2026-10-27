package com.mindmap.profiling.dto;

import java.util.List;
import java.util.UUID;

/**
 * Complete administrative psychological profile data transfer object.
 * Contains exhaustive diagnostic metric breakdowns for clinical and administrative review.
 *
 * @param userId            the target user UUID
 * @param userHandle        the target user's handle or username
 * @param isComplete        whether the user has completed all profiling levels
 * @param overallConfidence overall profile confidence score (0-100)
 * @param level1Scores      Big Five personality dimension scores
 * @param level2Scores      Ambiguity and dark theme cognitive tolerance scores
 * @param level3Scores      Temperament axis scores
 * @param temperament       Level 3 temperament classification and archetype distribution
 * @param safetyFlags       any safety or crisis flags triggered during assessments
 * @param disclaimer        clinical and ethical review notice
 */
public record FullProfileResponse(
        UUID userId,
        String userHandle,
        boolean isComplete,
        Double overallConfidence,
        LevelScoresResponse level1Scores,
        LevelScoresResponse level2Scores,
        LevelScoresResponse level3Scores,
        TemperamentResponse temperament,
        List<String> safetyFlags,
        String disclaimer
) {
    public static final String ADMIN_DISCLAIMER =
            "CONFIDENTIAL: This profile contains sensitive psychological metric data intended strictly "
                    + "for authorized administrators and mental wellness professionals. Handle with care under applicable privacy laws.";
}
