package com.mindmap.assessment.dto;

/**
 * Representation of progress within an assessment session.
 */
public record ProgressDto(
        int answered,
        int total,
        double percentComplete
) {
    public static ProgressDto of(int answered, int total) {
        double percent = total > 0 ? Math.round(((double) answered / total) * 1000.0) / 10.0 : 0.0;
        return new ProgressDto(answered, total, percent);
    }
}
