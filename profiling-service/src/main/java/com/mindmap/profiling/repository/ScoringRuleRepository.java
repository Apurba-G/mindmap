package com.mindmap.profiling.repository;

import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.ScoringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ScoringRule}.
 */
@Repository
public interface ScoringRuleRepository extends JpaRepository<ScoringRule, UUID> {

    /**
     * Retrieves all active scoring rules for a specific assessment level.
     *
     * @param level the assessment level
     * @return list of active rules
     */
    List<ScoringRule> findByLevelAndIsActiveTrue(ProfileLevel level);

    /**
     * Retrieves an active scoring rule for a specific level and trait key.
     *
     * @param level    the assessment level
     * @param traitKey the trait key
     * @return optional containing the active rule
     */
    Optional<ScoringRule> findByLevelAndTraitKeyAndIsActiveTrue(ProfileLevel level, String traitKey);

    /**
     * Retrieves all rules for a specific assessment level regardless of active status.
     *
     * @param level the assessment level
     * @return list of scoring rules
     */
    List<ScoringRule> findByLevel(ProfileLevel level);

    /**
     * Retrieves all active scoring rules across all levels.
     *
     * @return list of active rules
     */
    List<ScoringRule> findByIsActiveTrue();
}
