package com.mindmap.assessment.controller;

import com.mindmap.assessment.dto.AdminQuestionRequest;
import com.mindmap.assessment.dto.AssessmentStatsResponse;
import com.mindmap.assessment.dto.QuestionDto;
import com.mindmap.assessment.entity.AnswerOption;
import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.Question;
import com.mindmap.assessment.entity.SafetyFlag;
import com.mindmap.assessment.entity.SessionStatus;
import com.mindmap.assessment.repository.AssessmentSessionRepository;
import com.mindmap.assessment.repository.QuestionRepository;
import com.mindmap.assessment.repository.SafetyFlagRepository;
import com.mindmap.assessment.security.SecurityUtils;
import com.mindmap.assessment.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for administrative management of psychological assessment questions,
 * system statistics, and review of safety flags.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAssessmentController {

    private final QuestionRepository questionRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final SafetyFlagRepository safetyFlagRepository;
    private final SessionService sessionService;

    /**
     * Lists all questions configured across all assessment levels.
     */
    @GetMapping("/questions")
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        log.info("REST (Admin): Fetching all active questions");
        List<Question> questions = questionRepository.findByIsActiveTrueOrderByLevelAscOrdinalAsc();
        List<QuestionDto> dtos = questions.stream()
                .map(sessionService::toQuestionDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Creates a new question with associated answer choices.
     */
    @PostMapping("/questions")
    @Transactional
    public ResponseEntity<QuestionDto> createQuestion(@Valid @RequestBody AdminQuestionRequest request) {
        log.info("REST (Admin): Creating new question for level {}, part {}", request.level(), request.part());

        Question question = Question.builder()
                .level(request.level())
                .part(request.part())
                .questionType(request.questionType())
                .ordinal(request.ordinal())
                .questionText(request.questionText())
                .scenarioText(request.scenarioText())
                .contentWarning(request.contentWarning())
                .isSkippable(request.isSkippable())
                .timeLimitSec(request.timeLimitSec())
                .isActive(true)
                .build();

        if (request.followUpOf() != null) {
            questionRepository.findById(request.followUpOf()).ifPresent(question::setFollowUpOf);
        }

        if (request.options() != null) {
            for (AdminQuestionRequest.AdminOptionRequest optReq : request.options()) {
                AnswerOption option = AnswerOption.builder()
                        .optionLabel(optReq.optionLabel())
                        .optionText(optReq.optionText())
                        .traitKey(optReq.traitKey())
                        .scoreWeight(optReq.scoreWeight() != null ? optReq.scoreWeight() : 1.0)
                        .isSafetyFlag(optReq.isSafetyFlag())
                        .build();
                question.addOption(option);
            }
        }

        Question saved = questionRepository.save(question);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.toQuestionDto(saved));
    }

    /**
     * Returns operational statistics including session volumes by level and pending safety flags.
     */
    @GetMapping("/stats")
    public ResponseEntity<AssessmentStatsResponse> getStats() {
        log.info("REST (Admin): Fetching assessment statistics");

        long totalSessions = sessionRepository.count();
        long inProgress = sessionRepository.countByStatus(SessionStatus.IN_PROGRESS);
        long completed = sessionRepository.countByStatus(SessionStatus.COMPLETED);
        long abandoned = sessionRepository.countByStatus(SessionStatus.ABANDONED);
        long unreviewedSafety = safetyFlagRepository.countByReviewedFalse();

        Map<String, Long> byLevel = new HashMap<>();
        for (AssessmentLevel level : AssessmentLevel.values()) {
            byLevel.put(level.name(), sessionRepository.countByLevel(level));
        }

        AssessmentStatsResponse response = new AssessmentStatsResponse(
                totalSessions,
                inProgress,
                completed,
                abandoned,
                unreviewedSafety,
                byLevel
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves unreviewed psychological safety flags.
     */
    @GetMapping("/safety-flags")
    public ResponseEntity<List<SafetyFlag>> getUnreviewedSafetyFlags() {
        log.info("REST (Admin): Fetching unreviewed safety flags");
        List<SafetyFlag> flags = safetyFlagRepository.findByReviewedFalseOrderByCreatedAtDesc();
        return ResponseEntity.ok(flags);
    }

    /**
     * Marks a safety flag as reviewed by the administrator.
     */
    @PostMapping("/safety-flags/{id}/review")
    @Transactional
    public ResponseEntity<SafetyFlag> reviewSafetyFlag(@PathVariable("id") UUID flagId) {
        log.info("REST (Admin): Marking safety flag {} as reviewed", flagId);
        String adminEmail = SecurityUtils.getCurrentUserPrincipal()
                .map(p -> p.getUsername())
                .orElse("admin");

        SafetyFlag flag = safetyFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Safety flag not found: " + flagId));

        flag.setReviewed(true);
        flag.setReviewedBy(adminEmail);
        SafetyFlag saved = safetyFlagRepository.save(flag);

        return ResponseEntity.ok(saved);
    }
}
