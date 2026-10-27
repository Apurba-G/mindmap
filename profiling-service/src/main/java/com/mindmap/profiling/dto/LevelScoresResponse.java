package com.mindmap.profiling.dto;

import java.util.Map;

/**
 * Score breakdown for an entire assessment level across all measured traits.
 *
 * @param level  the assessment level identifier
 * @param traits mapping of trait key to its calculated scored trait
 */
public record LevelScoresResponse(
        String level,
        Map<String, ScoredTrait> traits
) {
}
