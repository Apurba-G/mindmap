package com.mindmap.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.assessment.config.EncryptionConfig;
import com.mindmap.assessment.dto.ProgressDto;
import com.mindmap.assessment.dto.QuestionDto;
import com.mindmap.assessment.dto.SafetyAlert;
import com.mindmap.assessment.dto.SubmitAnswerRequest;
import com.mindmap.assessment.dto.SubmitAnswerResponse;
import com.mindmap.assessment.entity.AnswerOption;
import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentPart;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.entity.Question;
import com.mindmap.assessment.entity.QuestionType;
import com.mindmap.assessment.entity.SessionStatus;
import com.mindmap.assessment.entity.UserAnswer;
import com.mindmap.assessment.exception.QuestionAlreadyAnsweredException;
import com.mindmap.assessment.exception.QuestionNotSkippableException;
import com.mindmap.assessment.repository.AnswerOptionRepository;
import com.mindmap.assessment.repository.AssessmentSessionRepository;
import com.mindmap.assessment.repository.QuestionRepository;
import com.mindmap.assessment.repository.UserAnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AssessmentSessionRepository sessionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;
    @Mock
    private UserAnswerRepository userAnswerRepository;
    @Mock
    private RedisSessionService redisSessionService;
    @Mock
    private SafetyService safetyService;
    @Mock
    private CompletionService completionService;
    @Mock
    private EncryptionConfig encryptionConfig;
    @Mock
    private SessionService sessionService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnswerService answerService;

    private UUID userId;
    private UUID sessionId;
    private UUID questionId;
    private UUID optionId;
    private AssessmentSession session;
    private Question question;
    private AnswerOption option;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        optionId = UUID.randomUUID();

        session = AssessmentSession.builder()
                .id(sessionId)
                .userId(userId)
                .level(AssessmentLevel.LEVEL_1)
                .status(SessionStatus.IN_PROGRESS)
                .build();

        question = Question.builder()
                .id(questionId)
                .level(AssessmentLevel.LEVEL_1)
                .part(AssessmentPart.MAIN)
                .questionType(QuestionType.SINGLE_CHOICE)
                .ordinal(1)
                .questionText("Question 1")
                .isSkippable(false)
                .build();

        option = AnswerOption.builder()
                .id(optionId)
                .question(question)
                .optionLabel("A")
                .optionText("Option A")
                .traitKey("EXTROVERSION")
                .scoreWeight(1.0)
                .isSafetyFlag(false)
                .build();
    }

    @Test
    @DisplayName("Successfully submit answer and advance progress")
    void shouldSubmitAnswerSuccessfully() throws Exception {
        SubmitAnswerRequest req = new SubmitAnswerRequest(questionId, optionId, 1200);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(questionRepository.findByIdWithOptions(questionId)).thenReturn(question);
        when(userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(false);
        when(answerOptionRepository.findById(optionId)).thenReturn(Optional.of(option));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(encryptionConfig.encrypt(any())).thenReturn(new byte[]{1, 2, 3});
        when(safetyService.checkForSafetyFlags(eq(userId), eq(sessionId), eq(questionId), eq(option)))
                .thenReturn(Optional.empty());
        when(questionRepository.findByLevelWithActiveOptions(AssessmentLevel.LEVEL_1))
                .thenReturn(List.of(question));
        when(userAnswerRepository.countBySessionId(sessionId)).thenReturn(1L);

        SubmitAnswerResponse response = answerService.submitAnswer(userId, sessionId, req);

        assertTrue(response.accepted());
        assertNotNull(response.progress());
        assertEquals(1, response.progress().answered());
        assertEquals(1, response.progress().total());
        assertNull(response.safetyAlert());

        verify(userAnswerRepository).save(any(UserAnswer.class));
        verify(completionService).completeLevel(userId, sessionId);
    }

    @Test
    @DisplayName("Should throw QuestionAlreadyAnsweredException when re-submitting")
    void shouldThrowWhenAlreadyAnswered() {
        SubmitAnswerRequest req = new SubmitAnswerRequest(questionId, optionId, 1200);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(questionRepository.findByIdWithOptions(questionId)).thenReturn(question);
        when(userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(true);

        assertThrows(QuestionAlreadyAnsweredException.class, () ->
                answerService.submitAnswer(userId, sessionId, req));
    }

    @Test
    @DisplayName("Should reject skipping non-skippable question")
    void shouldRejectSkipForMandatoryQuestion() {
        question.setSkippable(false);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(questionRepository.findByIdWithOptions(questionId)).thenReturn(question);

        assertThrows(QuestionNotSkippableException.class, () ->
                answerService.skipQuestion(userId, sessionId, questionId));
    }

    @Test
    @DisplayName("Should allow skipping skippable question")
    void shouldAllowSkippingQuestion() throws Exception {
        question.setSkippable(true);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(questionRepository.findByIdWithOptions(questionId)).thenReturn(question);
        when(userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(encryptionConfig.encrypt(any())).thenReturn(new byte[]{1, 2, 3});
        when(questionRepository.findByLevelWithActiveOptions(AssessmentLevel.LEVEL_1))
                .thenReturn(List.of(question));
        when(userAnswerRepository.countBySessionId(sessionId)).thenReturn(1L);

        SubmitAnswerResponse response = answerService.skipQuestion(userId, sessionId, questionId);

        assertTrue(response.accepted());
        assertEquals(1, response.progress().answered());
        verify(userAnswerRepository).save(any(UserAnswer.class));
    }

    @Test
    @DisplayName("Should return SafetyAlert when option triggers distress flag")
    void shouldReturnSafetyAlertWhenOptionTriggersFlag() throws Exception {
        option.setSafetyFlag(true);
        SubmitAnswerRequest req = new SubmitAnswerRequest(questionId, optionId, 1500);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(questionRepository.findByIdWithOptions(questionId)).thenReturn(question);
        when(userAnswerRepository.existsBySessionIdAndQuestionId(sessionId, questionId)).thenReturn(false);
        when(answerOptionRepository.findById(optionId)).thenReturn(Optional.of(option));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(encryptionConfig.encrypt(any())).thenReturn(new byte[]{1, 2, 3});

        SafetyAlert alert = new SafetyAlert("SUPPORT", "Help is available", List.of("988"));
        when(safetyService.checkForSafetyFlags(eq(userId), eq(sessionId), eq(questionId), eq(option)))
                .thenReturn(Optional.of(alert));

        when(questionRepository.findByLevelWithActiveOptions(AssessmentLevel.LEVEL_1))
                .thenReturn(List.of(question));
        when(userAnswerRepository.countBySessionId(sessionId)).thenReturn(1L);

        SubmitAnswerResponse response = answerService.submitAnswer(userId, sessionId, req);

        assertTrue(response.accepted());
        assertNotNull(response.safetyAlert());
        assertEquals("SUPPORT", response.safetyAlert().type());
        assertEquals("Help is available", response.safetyAlert().message());
    }
}
