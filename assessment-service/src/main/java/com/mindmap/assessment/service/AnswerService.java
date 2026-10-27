package com.mindmap.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.assessment.config.EncryptionConfig;
import com.mindmap.assessment.dto.ProgressDto;
import com.mindmap.assessment.dto.QuestionDto;
import com.mindmap.assessment.dto.SafetyAlert;
import com.mindmap.assessment.dto.SubmitAnswerRequest;
import com.mindmap.assessment.dto.SubmitAnswerResponse;
import com.mindmap.assessment.entity.AnswerOption;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.entity.Question;
import com.mindmap.assessment.entity.SessionStatus;
import com.mindmap.assessment.entity.UserAnswer;
import com.mindmap.assessment.exception.InvalidSessionStateException;
import com.mindmap.assessment.exception.QuestionAlreadyAnsweredException;
import com.mindmap.assessment.exception.QuestionNotFoundException;
import com.mindmap.assessment.exception.QuestionNotSkippableException;
import com.mindmap.assessment.exception.SessionNotFoundException;
import com.mindmap.assessment.exception.UnauthorizedAssessmentAccessException;
import com.mindmap.assessment.repository.AnswerOptionRepository;
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
 * Service orchestrating user answer submissions, question skipping, safety detection,
 * and assessment progression.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AssessmentSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final RedisSessionService redisSessionService;
    private final SafetyService safetyService;
    private final CompletionService completionService;
    private final EncryptionConfig encryptionConfig;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    /**
     * Submits an answer for a question in an active session.
     *
     * @param userId    authenticated user ID
     * @param sessionId active session UUID
     * @param req       answer submission request payload
     * @return answer response with progress, next question, and optional safety alert
     */
    @Transactional
    public SubmitAnswerResponse submitAnswer(UUID userId, UUID sessionId, SubmitAnswerRequest req) {
        log.info("Submitting answer for user={}, session={}, question={}", userId, sessionId, req.questionId());

        // 1. Validate session is IN_PROGRESS and belongs to user
        AssessmentSession session = validateSession(userId, sessionId);

        // 2. Validate question belongs to this session's level
        Question question = questionRepository.findByIdWithOptions(req.questionId());
        if (question == null) {
            throw new QuestionNotFoundException(req.questionId());
        }
        if (question.getLevel() != session.getLevel()) {
            throw new IllegalArgumentException(
                    "Question " + req.questionId() + " does not belong to session level " + session.getLevel());
        }

        // 3. Check if already answered
        if (userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, req.questionId())) {
            throw new QuestionAlreadyAnsweredException(req.questionId());
        }

        // Validate selected option
        if (req.selectedOptionId() == null) {
            throw new IllegalArgumentException("Selected option ID is required. Use the skip endpoint to skip skippable questions.");
        }
        AnswerOption selectedOption = answerOptionRepository.findById(req.selectedOptionId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid option ID: " + req.selectedOptionId()));

        if (!selectedOption.getQuestion().getId().equals(question.getId())) {
            throw new IllegalArgumentException("Selected option does not belong to the target question");
        }

        // 4. Encrypt raw answer data
        byte[] encryptedRaw = buildEncryptedPayload(userId, sessionId, question, selectedOption, req.responseTimeMs());

        // 5. Save UserAnswer to DB
        UserAnswer userAnswer = UserAnswer.builder()
                .session(session)
                .question(question)
                .selectedOption(selectedOption)
                .wasSkipped(false)
                .responseTimeMs(req.responseTimeMs() != null ? req.responseTimeMs().longValue() : null)
                .answeredAt(LocalDateTime.now())
                .encryptedRaw(encryptedRaw)
                .build();
        userAnswerRepository.save(userAnswer);

        // 6. Update Redis session hash
        int responseTime = req.responseTimeMs() != null ? req.responseTimeMs() : 0;
        redisSessionService.recordAnswer(sessionId, question.getId(), selectedOption.getId(), responseTime);

        // 7. Check for safety flags
        Optional<SafetyAlert> safetyAlert = safetyService.checkForSafetyFlags(userId, sessionId, question.getId(), selectedOption);

        // 8. Determine next question & update progress
        List<Question> allQuestions = questionRepository.findByLevelWithActiveOptions(session.getLevel());
        int totalQuestions = allQuestions.size();
        long answeredCount = userAnswerRepository.countBySessionId(sessionId);

        redisSessionService.updateProgress(sessionId, (int) answeredCount, totalQuestions);
        ProgressDto progress = ProgressDto.of((int) answeredCount, totalQuestions);

        Question nextQuestionEntity = findNextUnansweredQuestion(allQuestions, sessionId);
        QuestionDto nextQuestionDto = nextQuestionEntity != null ? sessionService.toQuestionDto(nextQuestionEntity) : null;

        redisSessionService.setCurrentQuestion(sessionId, nextQuestionEntity != null ? nextQuestionEntity.getId() : null);

        // 9. If all questions answered, trigger completion
        if (nextQuestionEntity == null) {
            log.info("All {} questions answered for session {}. Triggering completion.", totalQuestions, sessionId);
            completionService.completeLevel(userId, sessionId);
        }

        return new SubmitAnswerResponse(true, progress, nextQuestionDto, safetyAlert.orElse(null));
    }

    /**
     * Skips a question in an active session (valid only for questions flagged as skippable).
     *
     * @param userId     authenticated user ID
     * @param sessionId  active session UUID
     * @param questionId the question to skip
     * @return answer response with updated progress and next question
     */
    @Transactional
    public SubmitAnswerResponse skipQuestion(UUID userId, UUID sessionId, UUID questionId) {
        log.info("Skipping question for user={}, session={}, question={}", userId, sessionId, questionId);

        // 1. Validate session
        AssessmentSession session = validateSession(userId, sessionId);

        // 2. Validate question
        Question question = questionRepository.findByIdWithOptions(questionId);
        if (question == null) {
            throw new QuestionNotFoundException(questionId);
        }
        if (question.getLevel() != session.getLevel()) {
            throw new IllegalArgumentException(
                    "Question " + questionId + " does not belong to session level " + session.getLevel());
        }

        // 3. Validate skippable
        if (!question.isSkippable()) {
            throw new QuestionNotSkippableException(questionId);
        }

        // 4. Check if already answered
        if (userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, questionId)) {
            throw new QuestionAlreadyAnsweredException(questionId);
        }

        // 5. Encrypt raw skip record
        byte[] encryptedRaw = buildEncryptedPayload(userId, sessionId, question, null, null);

        // 6. Save UserAnswer as skipped
        UserAnswer userAnswer = UserAnswer.builder()
                .session(session)
                .question(question)
                .selectedOption(null)
                .wasSkipped(true)
                .responseTimeMs(0L)
                .answeredAt(LocalDateTime.now())
                .encryptedRaw(encryptedRaw)
                .build();
        userAnswerRepository.save(userAnswer);

        // 7. Update Redis
        redisSessionService.recordAnswer(sessionId, questionId, null, 0);

        // 8. Calculate next question and progress
        List<Question> allQuestions = questionRepository.findByLevelWithActiveOptions(session.getLevel());
        int totalQuestions = allQuestions.size();
        long answeredCount = userAnswerRepository.countBySessionId(sessionId);

        redisSessionService.updateProgress(sessionId, (int) answeredCount, totalQuestions);
        ProgressDto progress = ProgressDto.of((int) answeredCount, totalQuestions);

        Question nextQuestionEntity = findNextUnansweredQuestion(allQuestions, sessionId);
        QuestionDto nextQuestionDto = nextQuestionEntity != null ? sessionService.toQuestionDto(nextQuestionEntity) : null;

        redisSessionService.setCurrentQuestion(sessionId, nextQuestionEntity != null ? nextQuestionEntity.getId() : null);

        // 9. If finished, trigger completion
        if (nextQuestionEntity == null) {
            log.info("All questions answered/skipped for session {}. Triggering completion.", sessionId);
            completionService.completeLevel(userId, sessionId);
        }

        return new SubmitAnswerResponse(true, progress, nextQuestionDto, null);
    }

    private AssessmentSession validateSession(UUID userId, UUID sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new UnauthorizedAssessmentAccessException("User does not have access to this assessment session");
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new InvalidSessionStateException("Session is not active (current status: " + session.getStatus() + ")");
        }

        return session;
    }

    private Question findNextUnansweredQuestion(List<Question> questions, UUID sessionId) {
        for (Question q : questions) {
            if (!userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, q.getId())) {
                return q;
            }
        }
        return null;
    }

    private byte[] buildEncryptedPayload(
            UUID userId,
            UUID sessionId,
            Question question,
            AnswerOption option,
            Integer responseTimeMs
    ) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("sessionId", sessionId.toString());
            payload.put("questionId", question.getId().toString());
            payload.put("questionText", question.getQuestionText());
            payload.put("timestamp", LocalDateTime.now().toString());

            if (option != null) {
                payload.put("optionId", option.getId().toString());
                payload.put("optionLabel", option.getOptionLabel());
                payload.put("optionText", option.getOptionText());
                payload.put("traitKey", option.getTraitKey());
                payload.put("scoreWeight", option.getScoreWeight());
            } else {
                payload.put("skipped", true);
            }

            if (responseTimeMs != null) {
                payload.put("responseTimeMs", responseTimeMs);
            }

            String json = objectMapper.writeValueAsString(payload);
            return encryptionConfig.encrypt(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to build answer payload for encryption", e);
            return new byte[0];
        }
    }
}
