package com.mindmap.assessment.dto;

import com.mindmap.assessment.entity.AssessmentLevel;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload to initiate a new assessment session for a level.
 */
public record CreateSessionRequest(
        @NotNull(message = "Assessment level is required")
        AssessmentLevel level
) {}
