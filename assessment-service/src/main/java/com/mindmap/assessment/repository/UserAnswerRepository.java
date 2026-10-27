package com.mindmap.assessment.repository;

import com.mindmap.assessment.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for UserAnswer entities.
 */
@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, UUID> {

    List<UserAnswer> findBySessionId(UUID sessionId);

    @Query("SELECT ua FROM UserAnswer ua JOIN FETCH ua.question LEFT JOIN FETCH ua.selectedOption WHERE ua.session.id = :sessionId")
    List<UserAnswer> findBySessionIdWithDetails(@Param("sessionId") UUID sessionId);

    long countBySessionId(UUID sessionId);

    Optional<UserAnswer> findBySessionIdAndQuestionId(UUID sessionId, UUID questionId);

    boolean existsBySessionIdAndQuestionId(UUID sessionId, UUID questionId);
}
