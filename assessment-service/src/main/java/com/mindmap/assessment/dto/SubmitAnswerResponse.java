package com.mindmap.assessment.dto;

/**
 * Response payload returned after an answer or skip action is submitted.
 */
public record SubmitAnswerResponse(
        boolean accepted,
        ProgressDto progress,
        QuestionDto nextQuestion,
        SafetyAlert safetyAlert
) {}
