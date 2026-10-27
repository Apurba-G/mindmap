package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.AnswerData;
import com.mindmap.profiling.dto.ConfidenceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfidenceCalculatorTest {

    private ConfidenceCalculator confidenceCalculator;

    @BeforeEach
    void setUp() {
        confidenceCalculator = new ConfidenceCalculator();
        ReflectionTestUtils.setField(confidenceCalculator, "defaultMinResponseMs", 1500L);
        ReflectionTestUtils.setField(confidenceCalculator, "defaultFastAnswerPenalty", 5.0);
        ReflectionTestUtils.setField(confidenceCalculator, "defaultContradictionPenalty", 10.0);
        ReflectionTestUtils.setField(confidenceCalculator, "defaultPatternPenalty", 25.0);
    }

    @Test
    @DisplayName("Penalizes fast answers submitted below the response threshold")
    void testFastAnswerPenalty() {
        List<AnswerData> answers = List.of(
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "openness", 5.0, 500, false, false), // Fast
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "openness", 6.0, 600, false, false), // Fast
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "openness", 7.0, 3000, false, false)
        );

        ConfidenceResult result = confidenceCalculator.calculateConfidence(answers, Collections.emptyList());

        assertEquals(2, result.fastAnswerCount());
        // Base 100 - (2 * 5.0) = 90.0
        assertEquals(90.0, result.overallConfidence());
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("FAST_ANSWERS_DETECTED")));
    }

    @Test
    @DisplayName("Detects contradictions when opposite extremes occur on same trait")
    void testContradictionDetection() {
        List<AnswerData> answers = List.of(
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "extraversion", 9.0, 3000, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "extraversion", -4.0, 3200, false, false)
        );

        ConfidenceResult result = confidenceCalculator.calculateConfidence(answers, Collections.emptyList());

        assertEquals(1, result.contradictionCount());
        // Base 100 - 10.0 = 90.0
        assertEquals(90.0, result.overallConfidence());
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("CONTRADICTION_DETECTED")));
    }

    @Test
    @DisplayName("Flags straightlining pattern when same option chosen >= 80% of times")
    void testStraightliningDetection() {
        UUID commonOption = UUID.randomUUID();
        List<AnswerData> answers = List.of(
                new AnswerData(UUID.randomUUID(), commonOption, "openness", 5.0, 2500, false, false),
                new AnswerData(UUID.randomUUID(), commonOption, "openness", 5.0, 2600, false, false),
                new AnswerData(UUID.randomUUID(), commonOption, "conscientiousness", 5.0, 2700, false, false),
                new AnswerData(UUID.randomUUID(), commonOption, "extraversion", 5.0, 2800, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "agreeableness", 3.0, 2900, false, false)
        );

        ConfidenceResult result = confidenceCalculator.calculateConfidence(answers, Collections.emptyList());

        assertTrue(result.patternFlag());
        // 4 out of 5 = 80%, penalty of 25.0
        assertTrue(result.overallConfidence() <= 75.0);
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("STRAIGHTLINING_DETECTED")));
    }
}
