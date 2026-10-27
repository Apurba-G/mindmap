package com.mindmap.profiling.dto;

import com.mindmap.profiling.entity.TemperamentType;
import java.util.Map;

/**
 * Encapsulates the complete computation results of Level 3 Temperament scoring.
 *
 * @param axisScores            scores for emotional_control, impulsiveness, and clarity_of_thinking
 * @param strongCalmPct         percentage affinity for Strong and Calm archetype
 * @param moderateTemperPct     percentage affinity for Moderate Temper archetype
 * @param lowControlAngryPct    percentage affinity for Low Control Angry archetype
 * @param lowControlConfusedPct percentage affinity for Low Control Confused archetype
 * @param primaryType           dominant archetype
 * @param secondaryType         secondary archetype if affinity exceeds 25%, null otherwise
 * @param explanationText       supportive, non-clinical behavioral narrative
 */
public record TemperamentCalculationResult(
        Map<String, ScoredTrait> axisScores,
        double strongCalmPct,
        double moderateTemperPct,
        double lowControlAngryPct,
        double lowControlConfusedPct,
        TemperamentType primaryType,
        TemperamentType secondaryType,
        String explanationText
) {
}
