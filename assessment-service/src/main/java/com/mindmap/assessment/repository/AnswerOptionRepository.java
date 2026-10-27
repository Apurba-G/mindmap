package com.mindmap.assessment.repository;

import com.mindmap.assessment.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for AnswerOption entities.
 */
@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, UUID> {

    List<AnswerOption> findByQuestionIdOrderByOptionLabelAsc(UUID questionId);
}
