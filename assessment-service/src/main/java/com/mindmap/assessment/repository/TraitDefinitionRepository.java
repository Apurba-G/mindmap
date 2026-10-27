package com.mindmap.assessment.repository;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.TraitDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for TraitDefinition entities.
 */
@Repository
public interface TraitDefinitionRepository extends JpaRepository<TraitDefinition, String> {

    List<TraitDefinition> findByLevel(AssessmentLevel level);
}
