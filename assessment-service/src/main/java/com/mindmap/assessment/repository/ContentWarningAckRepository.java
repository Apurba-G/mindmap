package com.mindmap.assessment.repository;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentPart;
import com.mindmap.assessment.entity.ContentWarningAck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for ContentWarningAck entities.
 */
@Repository
public interface ContentWarningAckRepository extends JpaRepository<ContentWarningAck, UUID> {

    Optional<ContentWarningAck> findByUserIdAndLevelAndPart(
            UUID userId, AssessmentLevel level, AssessmentPart part);

    boolean existsByUserIdAndLevelAndPartAndAcceptedTrue(
            UUID userId, AssessmentLevel level, AssessmentPart part);
}
