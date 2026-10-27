package com.mindmap.profiling.entity;

/**
 * Psychological profiling assessment level.
 */
public enum ProfileLevel {
    LEVEL_1,
    LEVEL_2,
    LEVEL_3;

    public static ProfileLevel fromString(String value) {
        if (value == null) {
            return null;
        }
        return ProfileLevel.valueOf(value.trim().toUpperCase());
    }
}
