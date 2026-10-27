package com.mindmap.assessment.entity;

/**
 * Psychological profiling assessment level.
 */
public enum AssessmentLevel {
    LEVEL_1,
    LEVEL_2,
    LEVEL_3;

    public AssessmentLevel getPreviousLevel() {
        return switch (this) {
            case LEVEL_1 -> null;
            case LEVEL_2 -> LEVEL_1;
            case LEVEL_3 -> LEVEL_2;
        };
    }

    public AssessmentLevel getNextLevel() {
        return switch (this) {
            case LEVEL_1 -> LEVEL_2;
            case LEVEL_2 -> LEVEL_3;
            case LEVEL_3 -> null;
        };
    }
}
