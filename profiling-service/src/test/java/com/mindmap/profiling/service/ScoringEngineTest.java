package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.AnswerData;
import com.mindmap.profiling.dto.ConfidenceResult;
import com.mindmap.profiling.dto.ScoredTrait;
import com.mindmap.profiling.dto.TemperamentCalculationResult;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.RuleType;
import com.mindmap.profiling.entity.ScoringRule;
import com.mindmap.profiling.entity.TemperamentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringEngineTest {

    private ScoringEngine scoringEngine;

    @BeforeEach
    void setUp() {
        scoringEngine = new ScoringEngine();
    }

    @Test
    @DisplayName("Level 1 Big Five scoring normalizes traits correctly")
    void testScoreLevel1NormalizesTraits() {
        List<AnswerData> answers = List.of(
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "openness", 8.0, 3000, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "openness", 7.0, 2500, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "conscientiousness", 10.0, 4000, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "extraversion", 5.0, 3200, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "agreeableness", 9.0, 3100, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "emotional_stability", 6.0, 2900, false, false)
        );

        ScoringRule opennessRule = ScoringRule.builder()
                .level(ProfileLevel.LEVEL_1)
                .traitKey("openness")
                .ruleName("Openness Rule")
                .ruleType(RuleType.WEIGHT)
                .parameters(Map.of("maxScore", 20.0, "minScore", 0.0))
                .isActive(true)
                .build();

        ConfidenceResult confidence = new ConfidenceResult(95.0, 0, 0, false, Collections.emptyList());

        Map<String, ScoredTrait> results = scoringEngine.scoreLevel1(answers, List.of(opennessRule), confidence);

        assertNotNull(results);
        assertEquals(5, results.size());

        ScoredTrait openness = results.get("openness");
        assertNotNull(openness);
        assertEquals(15.0, openness.rawScore());
        // 15.0 / 20.0 * 100 = 75.0
        assertEquals(75.0, openness.normalized());
        assertEquals(95.0, openness.confidence());
    }

    @Test
    @DisplayName("Level 2 scoring does not penalize skipped questions")
    void testScoreLevel2ExcludesSkipped() {
        List<AnswerData> answers = List.of(
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "ambiguity_tolerance", 8.0, 2500, false, false),
                new AnswerData(UUID.randomUUID(), null, "ambiguity_tolerance", 0.0, 0, true, false), // Skipped
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "dark_theme_tolerance", 6.0, 3000, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "moral_flexibility", 7.0, 2800, false, false)
        );

        ConfidenceResult confidence = new ConfidenceResult(90.0, 0, 0, false, Collections.emptyList());

        Map<String, ScoredTrait> results = scoringEngine.scoreLevel2(answers, Collections.emptyList(), confidence);

        assertNotNull(results);
        assertTrue(results.containsKey("ambiguity_tolerance"));
        assertTrue(results.containsKey("consistency"));

        // Only 1 answered item for ambiguity_tolerance, so maxScore is 10.0, raw is 8.0 -> 80.0
        ScoredTrait ambiguity = results.get("ambiguity_tolerance");
        assertEquals(8.0, ambiguity.rawScore());
        assertEquals(80.0, ambiguity.normalized());
    }

    @Test
    @DisplayName("Level 3 Temperament calculates softmax probabilities summing to 100%")
    void testScoreLevel3TemperamentSoftmax() {
        List<AnswerData> answers = List.of(
                // High emotional control
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "emotional_control", 9.0, 2500, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "emotional_control", 8.0, 2600, false, false),
                // Low raw impulsiveness (high control)
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "impulsiveness", 1.0, 2700, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "impulsiveness", 2.0, 2800, false, false),
                // High clarity
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "clarity_of_thinking", 9.0, 2400, false, false),
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "clarity_of_thinking", 8.0, 2500, false, false)
        );

        ConfidenceResult confidence = new ConfidenceResult(92.0, 0, 0, false, Collections.emptyList());

        TemperamentCalculationResult result = scoringEngine.scoreLevel3(answers, Collections.emptyList(), confidence);

        assertNotNull(result);
        assertEquals(TemperamentType.STRONG_AND_CALM, result.primaryType());

        double sum = result.strongCalmPct() + result.moderateTemperPct()
                + result.lowControlAngryPct() + result.lowControlConfusedPct();
        assertEquals(100.0, Math.round(sum * 100.0) / 100.0);

        assertTrue(result.strongCalmPct() > result.moderateTemperPct());
        assertTrue(result.strongCalmPct() > result.lowControlAngryPct());
        assertTrue(result.strongCalmPct() > result.lowControlConfusedPct());
        assertNotNull(result.explanationText());
        assertFalse(result.explanationText().isBlank());
    }
}
