package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.CreateScoringRuleRequest;
import com.mindmap.profiling.dto.ScoringRuleDto;
import com.mindmap.profiling.dto.UpdateScoringRuleRequest;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.ScoringRule;
import com.mindmap.profiling.exception.ScoringRuleNotFoundException;
import com.mindmap.profiling.repository.ScoringRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing administrative configuration of scoring rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringRuleService {

    private final ScoringRuleRepository scoringRuleRepository;

    /**
     * Retrieves all scoring rules.
     *
     * @return list of rule DTOs
     */
    @Transactional(readOnly = true)
    public List<ScoringRuleDto> getAllRules() {
        return scoringRuleRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Retrieves all active rules for a given level.
     *
     * @param level assessment level
     * @return list of rule DTOs
     */
    @Transactional(readOnly = true)
    public List<ScoringRuleDto> getRulesByLevel(ProfileLevel level) {
        return scoringRuleRepository.findByLevel(level).stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Retrieves a single scoring rule by its unique identifier.
     *
     * @param id rule identifier
     * @return rule DTO
     */
    @Transactional(readOnly = true)
    public ScoringRuleDto getRuleById(UUID id) {
        return scoringRuleRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ScoringRuleNotFoundException(id));
    }

    /**
     * Creates a new scoring rule.
     *
     * @param request creation request
     * @return created rule DTO
     */
    @Transactional
    public ScoringRuleDto createRule(CreateScoringRuleRequest request) {
        log.info("Creating new scoring rule: {} for level={}", request.ruleName(), request.level());

        ScoringRule rule = ScoringRule.builder()
                .level(request.level())
                .traitKey(request.traitKey().toLowerCase())
                .ruleName(request.ruleName())
                .ruleType(request.ruleType())
                .parameters(request.parameters() != null ? new HashMap<>(request.parameters()) : new HashMap<>())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .version(1)
                .build();

        ScoringRule saved = scoringRuleRepository.save(rule);
        return mapToDto(saved);
    }

    /**
     * Updates an existing scoring rule.
     *
     * @param id      rule identifier
     * @param request update request
     * @return updated rule DTO
     */
    @Transactional
    public ScoringRuleDto updateRule(UUID id, UpdateScoringRuleRequest request) {
        log.info("Updating scoring rule ID: {}", id);

        ScoringRule rule = scoringRuleRepository.findById(id)
                .orElseThrow(() -> new ScoringRuleNotFoundException(id));

        if (request.parameters() != null) {
            rule.setParameters(new HashMap<>(request.parameters()));
        }
        if (request.isActive() != null) {
            rule.setIsActive(request.isActive());
        }
        rule.setVersion(rule.getVersion() + 1);

        ScoringRule updated = scoringRuleRepository.save(rule);
        return mapToDto(updated);
    }

    /**
     * Deletes a scoring rule.
     *
     * @param id rule identifier
     */
    @Transactional
    public void deleteRule(UUID id) {
        log.info("Deleting scoring rule ID: {}", id);
        if (!scoringRuleRepository.existsById(id)) {
            throw new ScoringRuleNotFoundException(id);
        }
        scoringRuleRepository.deleteById(id);
    }

    private ScoringRuleDto mapToDto(ScoringRule entity) {
        return new ScoringRuleDto(
                entity.getId(),
                entity.getLevel(),
                entity.getTraitKey(),
                entity.getRuleName(),
                entity.getRuleType(),
                entity.getParameters() != null ? entity.getParameters() : Map.of(),
                Boolean.TRUE.equals(entity.getIsActive()),
                entity.getVersion() != null ? entity.getVersion() : 1
        );
    }
}
