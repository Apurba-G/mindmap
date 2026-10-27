package com.mindmap.assessment.repository;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for AssessmentSession entities.
 */
@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, UUID> {

    List<AssessmentSession> findByUserIdAndLevel(UUID userId, AssessmentLevel level);

    List<AssessmentSession> findByUserIdAndStatus(UUID userId, SessionStatus status);

    Optional<AssessmentSession> findFirstByUserIdAndLevelAndStatusOrderByStartedAtDesc(
            UUID userId, AssessmentLevel level, SessionStatus status);

    Optional<AssessmentSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(
            UUID userId, SessionStatus status);

    Optional<AssessmentSession> findFirstByUserIdAndLevelOrderByStartedAtDesc(
            UUID userId, AssessmentLevel level);

    boolean existsByUserIdAndLevelAndStatus(
            UUID userId, AssessmentLevel level, SessionStatus status);

    List<AssessmentSession> findByUserIdOrderByStartedAtAsc(UUID userId);

    long countByStatus(SessionStatus status);

    long countByLevel(AssessmentLevel level);
}
