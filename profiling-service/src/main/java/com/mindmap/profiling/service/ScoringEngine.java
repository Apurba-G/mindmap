package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.AnswerData;
import com.mindmap.profiling.dto.ConfidenceResult;
import com.mindmap.profiling.dto.ScoredTrait;
import com.mindmap.profiling.dto.TemperamentCalculationResult;
import com.mindmap.profiling.entity.ScoringRule;
import com.mindmap.profiling.entity.TemperamentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Core mathematical scoring engine for psychological profiling across Assessment Levels 1, 2, and 3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringEngine {

    public static final List<String> LEVEL_1_TRAITS = List.of(
            "openness",
            "conscientiousness",
            "extraversion",
            "agreeableness",
            "emotional_stability"
    );

    public static final List<String> LEVEL_2_TRAITS = List.of(
            "ambiguity_tolerance",
            "dark_theme_tolerance",
            "moral_flexibility",
            "consistency"
    );

    public static final List<String> LEVEL_3_AXES = List.of(
            "emotional_control",
            "impulsiveness",
            "clarity_of_thinking"
    );

    // Archetype coordinate vectors in 3D feature space: [emotional_control, low_impulsiveness, clarity_of_thinking]
    private static final double[] ARCHETYPE_STRONG_AND_CALM = {85.0, 85.0, 85.0};
    private static final double[] ARCHETYPE_MODERATE_TEMPER = {55.0, 55.0, 55.0};
    private static final double[] ARCHETYPE_LOW_CONTROL_ANGRY = {25.0, 20.0, 65.0};
    private static final double[] ARCHETYPE_LOW_CONTROL_CONFUSED = {20.0, 20.0, 20.0};

    /**
     * Executes Level 1 (Big Five) scoring.
     *
     * @param answers    submitted answer items
     * @param rules      active scoring rules for Level 1
     * @param confidence overall assessment confidence result
     * @return map of trait key to {@link ScoredTrait}
     */
    public Map<String, ScoredTrait> scoreLevel1(
            List<AnswerData> answers,
            List<ScoringRule> rules,
            ConfidenceResult confidence
    ) {
        log.info("Executing Level 1 (Big Five) scoring for {} answers", answers != null ? answers.size() : 0);
        Map<String, ScoredTrait> result = new HashMap<>();

        Map<String, ScoringRule> ruleMap = buildRuleMap(rules);
        Map<String, List<AnswerData>> answersByTrait = groupAnswersByTrait(answers);

        for (String trait : LEVEL_1_TRAITS) {
            List<AnswerData> traitAnswers = answersByTrait.getOrDefault(trait, Collections.emptyList());
            ScoringRule rule = ruleMap.get(trait);

            double rawScore = 0.0;
            int answeredCount = 0;
            int traitFastCount = 0;
            List<Double> weights = new ArrayList<>();

            for (AnswerData a : traitAnswers) {
                if (!a.wasSkipped()) {
                    rawScore += a.scoreWeight();
                    answeredCount++;
                    weights.add(a.scoreWeight());
                    if (a.responseTimeMs() > 0 && a.responseTimeMs() < 1500) {
                        traitFastCount++;
                    }
                }
            }

            // Determine maximum possible score
            double maxScore = resolveMaxScore(rule, answeredCount, 10.0, 50.0);
            double minScore = resolveMinScore(rule, 0.0);

            double normalized = normalizeScore(rawScore, minScore, maxScore);

            // Compute trait-specific confidence
            double traitConfidence = confidence != null ? confidence.overallConfidence() : 80.0;
            if (traitFastCount > 0) {
                traitConfidence = Math.max(10.0, traitConfidence - (traitFastCount * 3.0));
            }
            if (hasLocalContradiction(weights)) {
                traitConfidence = Math.max(10.0, traitConfidence - 12.0);
            }

            traitConfidence = roundTwoDecimals(traitConfidence);
            normalized = roundTwoDecimals(normalized);
            rawScore = roundTwoDecimals(rawScore);

            result.put(trait, new ScoredTrait(rawScore, normalized, traitConfidence));
        }

        return result;
    }

    /**
     * Executes Level 2 (Ambiguity + Dark Theme + Moral Flexibility + Consistency) scoring.
     *
     * @param answers    submitted answer items
     * @param rules      active scoring rules for Level 2
     * @param confidence overall assessment confidence result
     * @return map of trait key to {@link ScoredTrait}
     */
    public Map<String, ScoredTrait> scoreLevel2(
            List<AnswerData> answers,
            List<ScoringRule> rules,
            ConfidenceResult confidence
    ) {
        log.info("Executing Level 2 scoring for {} answers", answers != null ? answers.size() : 0);
        Map<String, ScoredTrait> result = new HashMap<>();

        Map<String, ScoringRule> ruleMap = buildRuleMap(rules);
        Map<String, List<AnswerData>> answersByTrait = groupAnswersByTrait(answers);

        // Score ambiguity_tolerance, dark_theme_tolerance, moral_flexibility
        List<String> contentTraits = List.of("ambiguity_tolerance", "dark_theme_tolerance", "moral_flexibility");

        for (String trait : contentTraits) {
            List<AnswerData> traitAnswers = answersByTrait.getOrDefault(trait, Collections.emptyList());
            ScoringRule rule = ruleMap.get(trait);

            double rawScore = 0.0;
            int answeredCount = 0;

            for (AnswerData a : traitAnswers) {
                // If question was skipped: do NOT penalize, exclude from denominator
                if (!a.wasSkipped()) {
                    rawScore += a.scoreWeight();
                    answeredCount++;
                }
            }

            // Exclude skipped questions from denominator: maxScore is scaled to answeredCount
            double maxScore = answeredCount > 0
                    ? resolveMaxScore(rule, answeredCount, 10.0, answeredCount * 10.0)
                    : 1.0;
            double minScore = resolveMinScore(rule, 0.0);

            double normalized = answeredCount > 0 ? normalizeScore(rawScore, minScore, maxScore) : 50.0;
            double traitConfidence = confidence != null ? confidence.overallConfidence() : 80.0;

            result.put(trait, new ScoredTrait(
                    roundTwoDecimals(rawScore),
                    roundTwoDecimals(normalized),
                    roundTwoDecimals(traitConfidence)
            ));
        }

        // Compute consistency score through internal concordance
        ScoredTrait consistencyTrait = computeConsistencyTrait(answers, confidence);
        result.put("consistency", consistencyTrait);

        return result;
    }

    /**
     * Executes Level 3 Temperament scoring across emotional control, impulsiveness, and clarity.
     * Computes Euclidean distances to archetypes and softmax probability distributions.
     *
     * @param answers    submitted answer items
     * @param rules      active scoring rules for Level 3
     * @param confidence overall assessment confidence result
     * @return {@link TemperamentCalculationResult} containing axis scores, percentages, and narrative
     */
    public TemperamentCalculationResult scoreLevel3(
            List<AnswerData> answers,
            List<ScoringRule> rules,
            ConfidenceResult confidence
    ) {
        log.info("Executing Level 3 Temperament scoring for {} answers", answers != null ? answers.size() : 0);

        Map<String, ScoringRule> ruleMap = buildRuleMap(rules);
        Map<String, List<AnswerData>> answersByTrait = groupAnswersByTrait(answers);
        Map<String, ScoredTrait> axisScores = new HashMap<>();

        // 1. Calculate Emotional Control (0 - 100)
        double emotionalControlNorm = calculateAxis(
                answersByTrait.get("emotional_control"),
                ruleMap.get("emotional_control"),
                false,
                50.0
        );

        // 2. Calculate Impulsiveness (0 - 100 raw, then inverted so 100 = low impulsiveness)
        double rawImpulsivenessNorm = calculateAxis(
                answersByTrait.get("impulsiveness"),
                ruleMap.get("impulsiveness"),
                false,
                50.0
        );
        // Inverted: 100 means low impulsiveness (high impulse control)
        double lowImpulsiveness = Math.max(0.0, Math.min(100.0, 100.0 - rawImpulsivenessNorm));

        // 3. Calculate Clarity of Thinking (0 - 100)
        double clarityNorm = calculateAxis(
                answersByTrait.get("clarity_of_thinking"),
                ruleMap.get("clarity_of_thinking"),
                false,
                50.0
        );

        double baseConfidence = confidence != null ? confidence.overallConfidence() : 85.0;

        axisScores.put("emotional_control", new ScoredTrait(
                roundTwoDecimals(emotionalControlNorm),
                roundTwoDecimals(emotionalControlNorm),
                roundTwoDecimals(baseConfidence)
        ));
        axisScores.put("impulsiveness", new ScoredTrait(
                roundTwoDecimals(rawImpulsivenessNorm),
                roundTwoDecimals(lowImpulsiveness), // Inverted normalized score
                roundTwoDecimals(baseConfidence)
        ));
        axisScores.put("clarity_of_thinking", new ScoredTrait(
                roundTwoDecimals(clarityNorm),
                roundTwoDecimals(clarityNorm),
                roundTwoDecimals(baseConfidence)
        ));

        // Coordinate in [emotional_control, low_impulsiveness, clarity_of_thinking]
        double[] userCoords = {emotionalControlNorm, lowImpulsiveness, clarityNorm};

        // Euclidean distances to the 4 archetypes
        double dStrongCalm = euclideanDistance(userCoords, ARCHETYPE_STRONG_AND_CALM);
        double dModerateTemper = euclideanDistance(userCoords, ARCHETYPE_MODERATE_TEMPER);
        double dLowAngry = euclideanDistance(userCoords, ARCHETYPE_LOW_CONTROL_ANGRY);
        double dLowConfused = euclideanDistance(userCoords, ARCHETYPE_LOW_CONTROL_CONFUSED);

        // Softmax normalization over negative scaled distances: weight_i = exp(-d_i / 22.0)
        double scale = 22.0;
        double wStrongCalm = Math.exp(-dStrongCalm / scale);
        double wModerateTemper = Math.exp(-dModerateTemper / scale);
        double wLowAngry = Math.exp(-dLowAngry / scale);
        double wLowConfused = Math.exp(-dLowConfused / scale);

        double sumWeights = wStrongCalm + wModerateTemper + wLowAngry + wLowConfused;
        if (sumWeights == 0.0) {
            sumWeights = 1.0;
        }

        double strongCalmPct = roundTwoDecimals((wStrongCalm / sumWeights) * 100.0);
        double moderateTemperPct = roundTwoDecimals((wModerateTemper / sumWeights) * 100.0);
        double lowAngryPct = roundTwoDecimals((wLowAngry / sumWeights) * 100.0);
        double lowConfusedPct = roundTwoDecimals((wLowConfused / sumWeights) * 100.0);

        // Adjust rounding residue to ensure exact 100.0% sum
        double totalPct = strongCalmPct + moderateTemperPct + lowAngryPct + lowConfusedPct;
        double delta = roundTwoDecimals(100.0 - totalPct);
        strongCalmPct = roundTwoDecimals(strongCalmPct + delta);

        // Determine dominant archetype
        Map<TemperamentType, Double> distribution = new HashMap<>();
        distribution.put(TemperamentType.STRONG_AND_CALM, strongCalmPct);
        distribution.put(TemperamentType.MODERATE_TEMPER, moderateTemperPct);
        distribution.put(TemperamentType.LOW_CONTROL_ANGRY, lowAngryPct);
        distribution.put(TemperamentType.LOW_CONTROL_CONFUSED, lowConfusedPct);

        TemperamentType primaryType = TemperamentType.STRONG_AND_CALM;
        double maxPct = -1.0;
        for (Map.Entry<TemperamentType, Double> entry : distribution.entrySet()) {
            if (entry.getValue() > maxPct) {
                maxPct = entry.getValue();
                primaryType = entry.getKey();
            }
        }

        // Secondary type if exceeds 25% (and not primary)
        TemperamentType secondaryType = null;
        double highestSecondary = 25.0;
        for (Map.Entry<TemperamentType, Double> entry : distribution.entrySet()) {
            if (entry.getKey() != primaryType && entry.getValue() > highestSecondary) {
                highestSecondary = entry.getValue();
                secondaryType = entry.getKey();
            }
        }

        // Generate narrative explanation
        String explanationText = buildExplanationText(primaryType, secondaryType, distribution);

        return new TemperamentCalculationResult(
                axisScores,
                strongCalmPct,
                moderateTemperPct,
                lowAngryPct,
                lowConfusedPct,
                primaryType,
                secondaryType,
                explanationText
        );
    }

    private double calculateAxis(
            List<AnswerData> answers,
            ScoringRule rule,
            boolean invert,
            double defaultValue
    ) {
        if (answers == null || answers.isEmpty()) {
            return defaultValue;
        }

        double rawScore = 0.0;
        int count = 0;
        for (AnswerData a : answers) {
            if (!a.wasSkipped()) {
                rawScore += a.scoreWeight();
                count++;
            }
        }

        if (count == 0) {
            return defaultValue;
        }

        double maxScore = resolveMaxScore(rule, count, 10.0, count * 10.0);
        double minScore = resolveMinScore(rule, 0.0);
        double normalized = normalizeScore(rawScore, minScore, maxScore);

        return invert ? (100.0 - normalized) : normalized;
    }

    private double euclideanDistance(double[] v1, double[] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private String buildExplanationText(
            TemperamentType primaryType,
            TemperamentType secondaryType,
            Map<TemperamentType, Double> distribution
    ) {
        StringBuilder sb = new StringBuilder();

        switch (primaryType) {
            case STRONG_AND_CALM -> sb.append("Under pressure, you demonstrate strong emotional regulation, clear discernment, and deliberate composure. You navigate stressful interactions with calm resolve.");
            case MODERATE_TEMPER -> sb.append("You generally maintain composure and balance, though under acute stress or sustained ambiguity, you may experience brief surges of intensity before recentering.");
            case LOW_CONTROL_ANGRY -> sb.append("You tend to react quickly to perceived threats or sudden challenges with focused energy. While your cognitive clarity remains intact, heightened physiological arousal can accelerate immediate reactions.");
            case LOW_CONTROL_CONFUSED -> sb.append("In high-pressure situations, you may find it challenging to filter conflicting stimuli, experiencing emotional overload alongside cognitive fog. Grounding techniques provide immediate stability.");
        }

        if (secondaryType != null) {
            double secPct = distribution.getOrDefault(secondaryType, 0.0);
            sb.append(" Additionally, your profile reveals secondary affinity with ")
                    .append(secondaryType.name().replace('_', ' ').toLowerCase(Locale.ROOT))
                    .append(String.format(" (%.1f%%)", secPct))
                    .append(", indicating situational adaptability depending on environmental context and support.");
        }

        return sb.toString();
    }

    private ScoredTrait computeConsistencyTrait(List<AnswerData> answers, ConfidenceResult confidence) {
        if (answers == null || answers.size() < 2) {
            return new ScoredTrait(100.0, 100.0, confidence != null ? confidence.overallConfidence() : 90.0);
        }

        // Compare answer pairs on the same trait for directional concordance
        Map<String, List<AnswerData>> byTrait = groupAnswersByTrait(answers);
        int totalPairs = 0;
        int concordantPairs = 0;

        for (List<AnswerData> traitList : byTrait.values()) {
            List<AnswerData> valid = traitList.stream().filter(a -> !a.wasSkipped()).toList();
            for (int i = 0; i < valid.size() - 1; i++) {
                for (int j = i + 1; j < valid.size(); j++) {
                    totalPairs++;
                    double w1 = valid.get(i).scoreWeight();
                    double w2 = valid.get(j).scoreWeight();
                    // Concordant if weights share direction or both near neutral (|w| <= 1.0)
                    if ((w1 >= 0 && w2 >= 0) || (w1 <= 0 && w2 <= 0) || (Math.abs(w1 - w2) <= 3.0)) {
                        concordantPairs++;
                    }
                }
            }
        }

        double concordanceRatio = totalPairs > 0 ? (double) concordantPairs / totalPairs : 0.90;
        double rawConsistency = roundTwoDecimals(concordanceRatio * 100.0);
        double normalized = Math.max(0.0, Math.min(100.0, rawConsistency));
        double traitConfidence = confidence != null ? confidence.overallConfidence() : 85.0;

        return new ScoredTrait(rawConsistency, normalized, traitConfidence);
    }

    private Map<String, ScoringRule> buildRuleMap(List<ScoringRule> rules) {
        Map<String, ScoringRule> map = new HashMap<>();
        if (rules != null) {
            for (ScoringRule rule : rules) {
                if (rule.getTraitKey() != null) {
                    map.put(rule.getTraitKey().toLowerCase(Locale.ROOT), rule);
                }
            }
        }
        return map;
    }

    private Map<String, List<AnswerData>> groupAnswersByTrait(List<AnswerData> answers) {
        Map<String, List<AnswerData>> map = new HashMap<>();
        if (answers != null) {
            for (AnswerData a : answers) {
                if (a.traitKey() != null) {
                    map.computeIfAbsent(a.traitKey().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(a);
                }
            }
        }
        return map;
    }

    private double resolveMaxScore(ScoringRule rule, int count, double defaultPerItem, double fallback) {
        if (rule != null && rule.getParameters() != null) {
            Object max = rule.getParameters().get("maxScore");
            if (max instanceof Number n) {
                return n.doubleValue();
            }
        }
        return count > 0 ? (count * defaultPerItem) : fallback;
    }

    private double resolveMinScore(ScoringRule rule, double fallback) {
        if (rule != null && rule.getParameters() != null) {
            Object min = rule.getParameters().get("minScore");
            if (min instanceof Number n) {
                return n.doubleValue();
            }
        }
        return fallback;
    }

    private double normalizeScore(double raw, double min, double max) {
        if (max <= min) {
            return 50.0;
        }
        double normalized = ((raw - min) / (max - min)) * 100.0;
        return Math.max(0.0, Math.min(100.0, normalized));
    }

    private boolean hasLocalContradiction(List<Double> weights) {
        if (weights == null || weights.size() < 2) {
            return false;
        }
        double min = Collections.min(weights);
        double max = Collections.max(weights);
        return (min <= -3.0 && max >= 3.0) || (max - min >= 8.0);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
