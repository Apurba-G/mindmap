package com.mindmap.profiling.dto;

import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request payload for creating a new scoring rule.
 *
 * @param level      assessment level
 * @param traitKey   trait key
 * @param ruleName   rule name
 * @param ruleType   rule type
 * @param parameters rule parameters
 * @param isActive   active status
 */
public record CreateScoringRuleRequest(
        @NotNull ProfileLevel level,
        @NotBlank String traitKey,
        @NotBlank String ruleName,
        @NotNull RuleType ruleType,
        Map<String, Object> parameters,
        Boolean isActive
) {
}
