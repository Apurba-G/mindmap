package com.mindmap.assessment.dto;

import java.util.List;

/**
 * Psychological safety alert message and helpline info returned when crisis/distress options are triggered.
 */
public record SafetyAlert(
        String type,
        String message,
        List<String> helplineNumbers
) {}
