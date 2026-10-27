package com.mindmap.assessment.controller;

import com.mindmap.assessment.dto.SkipQuestionRequest;
import com.mindmap.assessment.dto.SubmitAnswerRequest;
import com.mindmap.assessment.dto.SubmitAnswerResponse;
import com.mindmap.assessment.security.SecurityUtils;
import com.mindmap.assessment.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for answering or skipping assessment questions.
 */
@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    /**
     * Submits an answer choice for a question within the given session.
     */
    @PostMapping("/{id}/answers")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable("id") UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: submit answer for session {}, question {}", sessionId, request.questionId());
        SubmitAnswerResponse response = answerService.submitAnswer(userId, sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Skips a skippable question within the session.
     */
    @PostMapping("/{id}/skip")
    public ResponseEntity<SubmitAnswerResponse> skipQuestion(
            @PathVariable("id") UUID sessionId,
            @RequestParam(name = "questionId", required = false) UUID paramQuestionId,
            @RequestBody(required = false) SkipQuestionRequest bodyRequest
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID targetQuestionId = (bodyRequest != null && bodyRequest.questionId() != null)
                ? bodyRequest.questionId()
                : paramQuestionId;

        if (targetQuestionId == null) {
            throw new IllegalArgumentException("Question ID must be provided in request body or as 'questionId' query parameter");
        }

        log.info("REST: skip question {} for session {}", targetQuestionId, sessionId);
        SubmitAnswerResponse response = answerService.skipQuestion(userId, sessionId, targetQuestionId);
        return ResponseEntity.ok(response);
    }
}
