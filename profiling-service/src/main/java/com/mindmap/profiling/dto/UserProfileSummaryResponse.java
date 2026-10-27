package com.mindmap.profiling.dto;

import java.util.List;

/**
 * User-facing psychological profile response.
 * Strictly presents non-clinical summaries and completion status without exposing raw diagnostic metrics.
 *
 * @param isComplete        whether the user has completed all profiling levels
 * @param highestLevel      the highest assessment level completed
 * @param overallConfidence the aggregate confidence percentage (0-100)
 * @param summaries         the list of visible level summaries
 * @param disclaimer        disclaimer reinforcing non-clinical self-awareness purpose
 */
public record UserProfileSummaryResponse(
        boolean isComplete,
        String highestLevel,
        Double overallConfidence,
        List<SummaryItemDto> summaries,
        String disclaimer
) {
    public static final String DEFAULT_DISCLAIMER =
            "MindMap profiling results are provided strictly for personal insight and self-awareness, "
                    + "not as a formal clinical evaluation or mental health diagnosis.";
}
