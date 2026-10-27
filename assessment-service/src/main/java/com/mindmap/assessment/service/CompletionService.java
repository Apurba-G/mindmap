package com.mindmap.assessment.service;

import com.mindmap.assessment.dto.AssessmentAnswerPayload;
import com.mindmap.assessment.dto.AssessmentCompletedEvent;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.entity.SessionStatus;
import com.mindmap.assessment.entity.UserAnswer;
import com.mindmap.assessment.exception.InvalidSessionStateException;
import com.mindmap.assessment.exception.SessionNotFoundException;
import com.mindmap.assessment.exception.UnauthorizedAssessmentAccessException;
import com.mindmap.assessment.repository.AssessmentSessionRepository;
import com.mindmap.assessment.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service managing assessment level completion workflows, final state transitions,
 * cache eviction, and dispatching completion events to Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompletionService {

    private final AssessmentSessionRepository sessionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final RedisSessionService redisSessionService;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Completes an assessment session, marks it COMPLETED, purges Redis state,
     * and publishes the AssessmentCompletedEvent to Kafka.
     *
     * @param userId    the authenticated user's ID
     * @param sessionId the assessment session ID
     */
    @Transactional
    public void completeLevel(UUID userId, UUID sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new UnauthorizedAssessmentAccessException("User cannot complete a session belonging to another user");
        }

        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.warn("Session {} is already marked COMPLETED", sessionId);
            return;
        }

        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new InvalidSessionStateException("Cannot complete an ABANDONED session");
        }

        LocalDateTime completedAt = LocalDateTime.now();
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(completedAt);

        if (session.getStartedAt() != null) {
            long seconds = Duration.between(session.getStartedAt(), completedAt).getSeconds();
            session.setTotalTimeSec((int) seconds);
        }

        sessionRepository.save(session);
        log.info("Assessment session {} for level {} marked COMPLETED", sessionId, session.getLevel());

        // Evict session from Redis cache
        redisSessionService.deleteSession(sessionId);

        // Fetch answers to assemble Kafka event
        List<UserAnswer> answers = userAnswerRepository.findBySessionIdWithDetails(sessionId);
        List<AssessmentAnswerPayload> answerPayloads = answers.stream()
                .map(ua -> new AssessmentAnswerPayload(
                        ua.getQuestion().getId(),
                        ua.getSelectedOption() != null ? ua.getSelectedOption().getId() : null,
                        ua.getSelectedOption() != null ? ua.getSelectedOption().getOptionLabel() : null,
                        ua.getSelectedOption() != null ? ua.getSelectedOption().getTraitKey() : null,
                        ua.getSelectedOption() != null ? ua.getSelectedOption().getScoreWeight() : 0.0,
                        ua.isWasSkipped(),
                        ua.getResponseTimeMs()
                ))
                .toList();

        AssessmentCompletedEvent event = new AssessmentCompletedEvent(
                session.getUserId(),
                session.getLevel(),
                session.getId(),
                answerPayloads,
                completedAt,
                session.getTotalTimeSec()
        );

        kafkaProducerService.publishAssessmentCompleted(event);
    }
}
