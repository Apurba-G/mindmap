package com.mindmap.assessment.repository;

import com.mindmap.assessment.entity.SafetyFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for SafetyFlag entities.
 */
@Repository
public interface SafetyFlagRepository extends JpaRepository<SafetyFlag, UUID> {

    List<SafetyFlag> findByReviewedFalseOrderByCreatedAtDesc();

    List<SafetyFlag> findBySessionId(UUID sessionId);

    long countByReviewedFalse();
}
