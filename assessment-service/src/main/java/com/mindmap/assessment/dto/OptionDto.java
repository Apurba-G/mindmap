package com.mindmap.assessment.dto;

import java.util.UUID;

/**
 * Data transfer record representing a question's answer option presented to the client.
 */
public record OptionDto(
        UUID id,
        String label,
        String text
) {}
