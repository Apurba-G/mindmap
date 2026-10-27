package com.mindmap.profiling.dto;

/**
 * Summarized narrative section for a particular assessment level.
 *
 * @param level       the assessment level name
 * @param summaryText the non-clinical, empathetic summary text
 * @param visibility  the visibility level (NONE, SHORT, DETAILED)
 */
public record SummaryItemDto(
        String level,
        String summaryText,
        String visibility
) {
}
