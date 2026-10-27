package com.mindmap.profiling.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Data representing an individual answer submitted in an assessment session.
 *
 * @param questionId       the unique identifier of the question
 * @param selectedOptionId the unique identifier of the chosen option
 * @param traitKey         the psychological trait key targeted by this question
 * @param scoreWeight      the numerical score weight contributed by the answer
 * @param responseTimeMs   the time taken to submit the response in milliseconds
 * @param wasSkipped       true if the question was explicitly skipped
 * @param isSafetyFlag     true if this response triggered a safety/crisis threshold
 */
public record AnswerData(
        @NotNull UUID questionId,
        UUID selectedOptionId,
        @NotNull String traitKey,
        double scoreWeight,
        long responseTimeMs,
        boolean wasSkipped,
        boolean isSafetyFlag
) {
}
