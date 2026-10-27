package com.mindmap.assessment.repository;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentPart;
import com.mindmap.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Question entities.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByLevelAndPartAndIsActiveTrueOrderByOrdinal(AssessmentLevel level, AssessmentPart part);

    List<Question> findByLevelAndIsActiveTrueOrderByOrdinal(AssessmentLevel level);

    long countByLevelAndIsActiveTrue(AssessmentLevel level);

    List<Question> findByIsActiveTrueOrderByLevelAscOrdinalAsc();

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.level = :level AND q.isActive = true ORDER BY q.ordinal ASC")
    List<Question> findByLevelWithActiveOptions(@Param("level") AssessmentLevel level);

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.id = :id")
    Question findByIdWithOptions(@Param("id") UUID id);
}
