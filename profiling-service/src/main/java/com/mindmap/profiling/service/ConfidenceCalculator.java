package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.AnswerData;
import com.mindmap.profiling.dto.ConfidenceResult;
import com.mindmap.profiling.entity.ScoringRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service calculating psychological profile confidence scores based on response times,
 * pattern detection (straightlining), and internal trait contradictions.
 */
@Slf4j
@Service
public class ConfidenceCalculator {

    @Value("${profiling.scoring.default-min-response-ms:1500}")
    private long defaultMinResponseMs;

    @Value("${profiling.scoring.fast-answer-penalty:5.0}")
    private double defaultFastAnswerPenalty;

    @Value("${profiling.scoring.contradiction-penalty:10.0}")
    private double defaultContradictionPenalty;

    @Value("${profiling.scoring.pattern-penalty:25.0}")
    private double defaultPatternPenalty;

    /**
     * Calculates the confidence result for an assessment submission.
     *
     * @param answers the submitted answer list
     * @param rules   applicable scoring rules for the level
     * @return {@link ConfidenceResult} with overall confidence and auditing flags
     */
    public ConfidenceResult calculateConfidence(List<AnswerData> answers, List<ScoringRule> rules) {
        if (answers == null || answers.isEmpty()) {
            return new ConfidenceResult(50.0, 0, 0, false, List.of("NO_ANSWERS_PROVIDED"));
        }

        List<String> flags = new ArrayList<>();
        double confidence = 100.0;

        // Extract rule parameters if present
        long minResponseMs = defaultMinResponseMs;
        double fastPenalty = defaultFastAnswerPenalty;
        double contradictionPenalty = defaultContradictionPenalty;
        double patternPenalty = defaultPatternPenalty;

        for (ScoringRule rule : rules) {
            Map<String, Object> params = rule.getParameters();
            if (params != null) {
                if (params.get("minResponseMs") instanceof Number n) {
                    minResponseMs = n.longValue();
                }
                if (params.get("fastAnswerPenalty") instanceof Number n) {
                    fastPenalty = n.doubleValue();
                }
                if (params.get("contradictionPenalty") instanceof Number n) {
                    contradictionPenalty = n.doubleValue();
                }
            }
        }

        // 1. Fast answer detection
        int fastAnswerCount = 0;
        int normalPacingCount = 0;
        for (AnswerData answer : answers) {
            if (!answer.wasSkipped() && answer.responseTimeMs() > 0) {
                if (answer.responseTimeMs() < minResponseMs) {
                    fastAnswerCount++;
                } else if (answer.responseTimeMs() >= 2000 && answer.responseTimeMs() <= 15000) {
                    normalPacingCount++;
                }
            }
        }

        if (fastAnswerCount > 0) {
            double penalty = Math.min(30.0, fastAnswerCount * fastPenalty);
            confidence -= penalty;
            flags.add("FAST_ANSWERS_DETECTED: " + fastAnswerCount + " responses under " + minResponseMs + "ms");
        }

        // 2. Contradiction detection per trait
        Map<String, List<Double>> traitWeights = new HashMap<>();
        for (AnswerData answer : answers) {
            if (!answer.wasSkipped() && answer.traitKey() != null) {
                traitWeights.computeIfAbsent(answer.traitKey(), k -> new ArrayList<>()).add(answer.scoreWeight());
            }
        }

        int contradictionCount = 0;
        for (Map.Entry<String, List<Double>> entry : traitWeights.entrySet()) {
            List<Double> weights = entry.getValue();
            if (weights.size() >= 2) {
                double min = Double.MAX_VALUE;
                double max = -Double.MAX_VALUE;
                for (double w : weights) {
                    if (w < min) min = w;
                    if (w > max) max = w;
                }
                // Contradiction: opposite polarities with significant amplitude or spread > 7.0
                if ((min <= -3.0 && max >= 3.0) || (max - min >= 8.0)) {
                    contradictionCount++;
                    flags.add("CONTRADICTION_DETECTED on trait: " + entry.getKey() + " (min=" + min + ", max=" + max + ")");
                }
            }
        }

        if (contradictionCount > 0) {
            double penalty = Math.min(40.0, contradictionCount * contradictionPenalty);
            confidence -= penalty;
        }

        // 3. Pattern detection (straightlining: selecting same option repeatedly)
        boolean patternFlag = false;
        long nonSkippedCount = answers.stream().filter(a -> !a.wasSkipped()).count();
        if (nonSkippedCount >= 5) {
            Map<UUID, Integer> optionFrequencies = new HashMap<>();
            for (AnswerData a : answers) {
                if (!a.wasSkipped() && a.selectedOptionId() != null) {
                    optionFrequencies.merge(a.selectedOptionId(), 1, Integer::sum);
                }
            }

            for (Map.Entry<UUID, Integer> entry : optionFrequencies.entrySet()) {
                double frequencyRatio = (double) entry.getValue() / nonSkippedCount;
                if (frequencyRatio >= 0.80) {
                    patternFlag = true;
                    confidence -= patternPenalty;
                    flags.add(String.format("STRAIGHTLINING_DETECTED: option %s selected in %.1f%% of responses",
                            entry.getKey(), frequencyRatio * 100));
                    break;
                }
            }
        }

        // 4. Timed pacing checks
        if (normalPacingCount >= (nonSkippedCount * 0.7) && fastAnswerCount == 0 && contradictionCount == 0) {
            confidence += 3.0; // Bonus for consistent, deliberate pacing
        }

        // Clamp between 10.0 and 100.0
        double finalConfidence = Math.max(10.0, Math.min(100.0, Math.round(confidence * 100.0) / 100.0));

        log.debug("Computed confidence: {} with {} fast answers, {} contradictions, patternFlag={}",
                finalConfidence, fastAnswerCount, contradictionCount, patternFlag);

        return new ConfidenceResult(finalConfidence, fastAnswerCount, contradictionCount, patternFlag, flags);
    }
}
