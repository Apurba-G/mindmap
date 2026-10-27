package com.mindmap.profiling.dto;

import java.util.List;

/**
 * Result of evaluating response patterns and metrics for assessment confidence.
 *
 * @param overallConfidence the resulting confidence percentage (0-100)
 * @param fastAnswerCount   number of answers submitted under the minimum response threshold
 * @param contradictionCount number of opposing extreme answers on identical trait dimensions
 * @param patternFlag       whether an unnatural pattern (e.g., straightlining) was detected
 * @param flags             descriptive warning flags for administrative auditing
 */
public record ConfidenceResult(
        double overallConfidence,
        int fastAnswerCount,
        int contradictionCount,
        boolean patternFlag,
        List<String> flags
) {
}
