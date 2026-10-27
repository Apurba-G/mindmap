package com.mindmap.assessment.service;

import com.mindmap.assessment.dto.OptionDto;
import com.mindmap.assessment.dto.QuestionDto;
import com.mindmap.assessment.dto.SessionData;
import com.mindmap.assessment.dto.SessionResponse;
import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.entity.Question;
import com.mindmap.assessment.entity.SessionStatus;
import com.mindmap.assessment.exception.InvalidSessionStateException;
import com.mindmap.assessment.exception.SessionNotFoundException;
import com.mindmap.assessment.exception.UnauthorizedAssessmentAccessException;
import com.mindmap.assessment.repository.AssessmentSessionRepository;
import com.mindmap.assessment.repository.QuestionRepository;
import com.mindmap.assessment.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing lifecycle operations for psychological assessment sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final AssessmentSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final LevelUnlockService levelUnlockService;
    private final RedisSessionService redisSessionService;

    /**
     * Creates a new assessment session for the user after validating level unlock rules
     * and ensuring no conflicting in-progress sessions.
     *
     * @param userId the user's UUID
     * @param level  the target assessment level
     * @return the initialized session response containing level questions
     */
    @Transactional
    public SessionResponse createSession(UUID userId, AssessmentLevel level) {
        log.info("Creating assessment session for userId={} at level={}", userId, level);

        // 1. Check level is unlocked
        levelUnlockService.validateLevelUnlocked(userId, level);

        // 2. Check no existing IN_PROGRESS session for this user at this level
        Optional<AssessmentSession> existing = sessionRepository
                .findFirstByUserIdAndLevelAndStatusOrderByStartedAtDesc(userId, level, SessionStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            throw new InvalidSessionStateException(
                    "An active session is already in progress for " + level + ". Please resume or abandon it.");
        }

        // 3. Load active questions for the level
        List<Question> questions = questionRepository.findByLevelWithActiveOptions(level);
        if (questions.isEmpty()) {
            throw new IllegalStateException("No active questions configured for level: " + level);
        }

        // 4. Create AssessmentSession in DB
        AssessmentSession session = AssessmentSession.builder()
                .userId(userId)
                .level(level)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();
        session = sessionRepository.save(session);

        // 5. Create Redis session hash
        UUID firstQuestionId = questions.get(0).getId();
        SessionData redisData = new SessionData(
                session.getId(),
                userId,
                level,
                0,
                questions.size(),
                firstQuestionId,
                System.currentTimeMillis(),
                new HashMap<>()
        );
        redisSessionService.createSession(session.getId(), redisData);

        // 6. Return session response
        List<QuestionDto> questionDtos = questions.stream()
                .map(this::toQuestionDto)
                .toList();

        return new SessionResponse(
                session.getId(),
                level,
                questions.size(),
                questionDtos,
                null
        );
    }

    /**
     * Resumes an existing IN_PROGRESS assessment session for the user.
     *
     * @param userId the user's UUID
     * @return SessionResponse with resumedFrom session ID
     */
    @Transactional
    public SessionResponse resumeSession(UUID userId) {
        log.info("Resuming active session for userId={}", userId);

        AssessmentSession session = sessionRepository
                .findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new SessionNotFoundException("No in-progress session found for user: " + userId));

        List<Question> questions = questionRepository.findByLevelWithActiveOptions(session.getLevel());
        List<QuestionDto> questionDtos = questions.stream()
                .map(this::toQuestionDto)
                .toList();

        // Check if Redis state exists; if not, re-populate from DB
        SessionData cached = redisSessionService.getSession(session.getId());
        if (cached == null) {
            long answeredCount = userAnswerRepository.countBySessionId(session.getId());
            UUID nextQId = determineNextQuestionId(questions, session.getId());
            SessionData rebuilt = new SessionData(
                    session.getId(),
                    userId,
                    session.getLevel(),
                    (int) answeredCount,
                    questions.size(),
                    nextQId,
                    session.getStartedAt() != null ? System.currentTimeMillis() : 0,
                    new HashMap<>()
            );
            redisSessionService.createSession(session.getId(), rebuilt);
        }

        return new SessionResponse(
                session.getId(),
                session.getLevel(),
                questions.size(),
                questionDtos,
                session.getId()
        );
    }

    /**
     * Abandons an in-progress session and purges in-memory caching.
     *
     * @param userId    the user's UUID
     * @param sessionId the session UUID
     */
    @Transactional
    public void abandonSession(UUID userId, UUID sessionId) {
        log.info("Abandoning session {} for userId={}", sessionId, userId);

        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new UnauthorizedAssessmentAccessException("User cannot abandon a session belonging to another user");
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new InvalidSessionStateException("Cannot abandon a session with status: " + session.getStatus());
        }

        session.setStatus(SessionStatus.ABANDONED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        redisSessionService.deleteSession(sessionId);
    }

    /**
     * Retrieves an existing session by ID.
     */
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID userId, UUID sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new UnauthorizedAssessmentAccessException("User cannot access a session belonging to another user");
        }

        List<Question> questions = questionRepository.findByLevelWithActiveOptions(session.getLevel());
        List<QuestionDto> questionDtos = questions.stream()
                .map(this::toQuestionDto)
                .toList();

        return new SessionResponse(
                session.getId(),
                session.getLevel(),
                questions.size(),
                questionDtos,
                null
        );
    }

    /**
     * Determines next unanswered question ID for rebuilding session state.
     */
    private UUID determineNextQuestionId(List<Question> questions, UUID sessionId) {
        for (Question q : questions) {
            if (!userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, q.getId())) {
                return q.getId();
            }
        }
        return null;
    }

    /**
     * Maps Question entity to QuestionDto record.
     */
    public QuestionDto toQuestionDto(Question q) {
        List<OptionDto> optionDtos = q.getOptions().stream()
                .map(opt -> new OptionDto(opt.getId(), opt.getOptionLabel(), opt.getOptionText()))
                .toList();

        return new QuestionDto(
                q.getId(),
                q.getOrdinal(),
                q.getQuestionText(),
                q.getScenarioText(),
                optionDtos,
                q.isSkippable(),
                q.getTimeLimitSec(),
                q.getContentWarning()
        );
    }
}
