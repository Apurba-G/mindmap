package com.mindmap.profiling.dto;

import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.RuleType;
import java.util.Map;
import java.util.UUID;

/**
 * Administrative DTO representation of a {@link com.mindmap.profiling.entity.ScoringRule}.
 *
 * @param id         the unique rule ID
 * @param level      the target assessment level
 * @param traitKey   the trait key affected by this rule
 * @param ruleName   a human-readable name for the rule
 * @param ruleType   the rule category (THRESHOLD, FORMULA, WEIGHT)
 * @param parameters arbitrary configuration parameters in JSON key-value format
 * @param isActive   whether the rule is currently applied in scoring computations
 * @param version    concurrency/audit version number
 */
public record ScoringRuleDto(
        UUID id,
        ProfileLevel level,
        String traitKey,
        String ruleName,
        RuleType ruleType,
        Map<String, Object> parameters,
        boolean isActive,
        int version
) {
}
