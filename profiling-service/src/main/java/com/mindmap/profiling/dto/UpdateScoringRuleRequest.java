package com.mindmap.profiling.dto;

import java.util.Map;

/**
 * Request payload for modifying an existing scoring rule.
 *
 * @param parameters updated parameters map
 * @param isActive   updated active status flag
 */
public record UpdateScoringRuleRequest(
        Map<String, Object> parameters,
        Boolean isActive
) {
}
