package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.ScoredTrait;
import com.mindmap.profiling.entity.ProfileLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryGeneratorTest {

    private SummaryGenerator summaryGenerator;

    @BeforeEach
    void setUp() {
        summaryGenerator = new SummaryGenerator();
    }

    @Test
    @DisplayName("Appends clinical disclaimer to generated summary")
    void testAppendsClinicalDisclaimer() {
        Map<String, ScoredTrait> scores = Map.of(
                "openness", new ScoredTrait(15.0, 75.0, 90.0),
                "conscientiousness", new ScoredTrait(12.0, 60.0, 92.0),
                "extraversion", new ScoredTrait(14.0, 70.0, 88.0),
                "agreeableness", new ScoredTrait(16.0, 80.0, 95.0),
                "emotional_stability", new ScoredTrait(13.0, 65.0, 89.0)
        );

        String summary = summaryGenerator.generateSummary(ProfileLevel.LEVEL_1, scores);

        assertNotNull(summary);
        assertTrue(summary.contains("For self-awareness only, not a clinical diagnosis."));
        assertTrue(summary.contains("openness"));
    }

    @Test
    @DisplayName("Level 3 summary does not contain clinical temperament labels")
    void testLevel3SummaryOmitsStigmatizingLabels() {
        Map<String, ScoredTrait> scores = Map.of(
                "emotional_control", new ScoredTrait(85.0, 85.0, 90.0),
                "impulsiveness", new ScoredTrait(15.0, 85.0, 90.0),
                "clarity_of_thinking", new ScoredTrait(85.0, 85.0, 90.0)
        );

        String summary = summaryGenerator.generateSummary(ProfileLevel.LEVEL_3, scores);

        assertNotNull(summary);
        assertFalse(summary.contains("STRONG_AND_CALM"));
        assertFalse(summary.contains("LOW_CONTROL_ANGRY"));
        assertTrue(summary.contains("Under pressure, you tend to"));
        assertTrue(summary.contains("This is a natural tendency, not a fixed trait."));
    }
}
