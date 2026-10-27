package com.mindmap.profiling.controller;

import com.mindmap.profiling.dto.CreateScoringRuleRequest;
import com.mindmap.profiling.dto.ScoringRuleDto;
import com.mindmap.profiling.dto.UpdateScoringRuleRequest;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.service.ScoringRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Administrative controller for configuring and updating scoring engine rules.
 */
@Slf4j
@RestController
@RequestMapping({"/admin/scoring-rules", "/api/v1/admin/scoring-rules"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ScoringRuleController {

    private final ScoringRuleService scoringRuleService;

    /**
     * Lists scoring rules, optionally filtered by assessment level.
     *
     * @param level optional level filter
     * @return list of scoring rule DTOs
     */
    @GetMapping
    public ResponseEntity<List<ScoringRuleDto>> getScoringRules(
            @RequestParam(value = "level", required = false) String level
    ) {
        if (level != null && !level.isBlank()) {
            ProfileLevel profileLevel = ProfileLevel.fromString(level);
            return ResponseEntity.ok(scoringRuleService.getRulesByLevel(profileLevel));
        }
        return ResponseEntity.ok(scoringRuleService.getAllRules());
    }

    /**
     * Retrieves a single scoring rule by ID.
     *
     * @param id rule identifier
     * @return scoring rule DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScoringRuleDto> getScoringRuleById(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(scoringRuleService.getRuleById(id));
    }

    /**
     * Creates a new scoring rule.
     *
     * @param request creation parameters
     * @return created scoring rule DTO
     */
    @PostMapping
    public ResponseEntity<ScoringRuleDto> createScoringRule(
            @Valid @RequestBody CreateScoringRuleRequest request
    ) {
        ScoringRuleDto created = scoringRuleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates parameters or status of an existing scoring rule.
     *
     * @param id      rule identifier
     * @param request update payload
     * @return updated scoring rule DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScoringRuleDto> updateScoringRule(
            @PathVariable("id") UUID id,
            @RequestBody UpdateScoringRuleRequest request
    ) {
        ScoringRuleDto updated = scoringRuleService.updateRule(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a scoring rule.
     *
     * @param id rule identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScoringRule(
            @PathVariable("id") UUID id
    ) {
        scoringRuleService.deleteRule(id);
    }
}
