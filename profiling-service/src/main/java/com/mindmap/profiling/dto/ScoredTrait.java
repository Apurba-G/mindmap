package com.mindmap.profiling.dto;

/**
 * Encapsulates scoring results for an individual psychological trait.
 *
 * @param rawScore   the accumulated raw score
 * @param normalized the score normalized to a 0-100 scale
 * @param confidence the confidence level (0-100) reflecting answering consistency and pacing
 */
public record ScoredTrait(
        double rawScore,
        double normalized,
        double confidence
) {
}
