package com.mindmap.profiling.dto;

import com.mindmap.profiling.entity.TemperamentType;
import java.util.Map;

/**
 * Detailed Level 3 temperament analysis and breakdown.
 *
 * @param primaryType          the dominant temperament classification
 * @param breakdownPercentages relative distribution across the four temperament archetypes (sums to 100%)
 * @param explanation          compassionate behavioral explanation of dominant/secondary traits
 * @param confidence           scoring confidence level (0-100)
 */
public record TemperamentResponse(
        TemperamentType primaryType,
        Map<TemperamentType, Double> breakdownPercentages,
        String explanation,
        double confidence
) {
}
