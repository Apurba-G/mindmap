package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.ScoredTrait;
import com.mindmap.profiling.entity.ProfileLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for generating compassionate, user-facing, non-clinical summary narratives.
 */
@Slf4j
@Service
public class SummaryGenerator {

    private static final String CLINICAL_DISCLAIMER_SUFFIX =
            " For self-awareness only, not a clinical diagnosis.";

    /**
     * Generates a supportive narrative summary for the completed profiling level.
     *
     * @param level  the completed assessment level
     * @param scores calculated scored traits for that level
     * @return friendly, non-stigmatizing summary text
     */
    public String generateSummary(ProfileLevel level, Map<String, ScoredTrait> scores) {
        if (scores == null || scores.isEmpty()) {
            return "Assessment complete. Your baseline data has been securely recorded." + CLINICAL_DISCLAIMER_SUFFIX;
        }

        StringBuilder sb = new StringBuilder();

        switch (level) {
            case LEVEL_1 -> generateLevel1Summary(sb, scores);
            case LEVEL_2 -> generateLevel2Summary(sb, scores);
            case LEVEL_3 -> generateLevel3Summary(sb, scores);
        }

        sb.append(CLINICAL_DISCLAIMER_SUFFIX);
        return sb.toString();
    }

    private void generateLevel1Summary(StringBuilder sb, Map<String, ScoredTrait> scores) {
        // High/moderate/balanced tendencies across Big Five traits
        ScoredTrait openness = scores.get("openness");
        ScoredTrait conscientiousness = scores.get("conscientiousness");
        ScoredTrait extraversion = scores.get("extraversion");
        ScoredTrait agreeableness = scores.get("agreeableness");
        ScoredTrait emotionalStability = scores.get("emotional_stability");

        sb.append("In this initial personality exploration, ");

        if (openness != null) {
            String tendency = categorizeTendency(openness.normalized());
            String desc = openness.normalized() >= 60.0
                    ? "appreciate fresh perspectives, imaginative concepts, and varied experiences"
                    : (openness.normalized() <= 40.0 ? "value practical realism, proven methods, and grounded clarity"
                    : "balance creative curiosity with pragmatic daily application");
            sb.append(String.format("you show a %s tendency toward openness, suggesting you %s. ", tendency, desc));
        }

        if (conscientiousness != null) {
            String tendency = categorizeTendency(conscientiousness.normalized());
            String desc = conscientiousness.normalized() >= 60.0
                    ? "bring systematic organization and focused follow-through to your goals"
                    : (conscientiousness.normalized() <= 40.0 ? "embrace flexible, adaptive rhythms rather than rigid structures"
                    : "balance structured planning with spontaneous flexibility");
            sb.append(String.format("You demonstrate a %s approach to conscientiousness, indicating that you %s. ", tendency, desc));
        }

        if (extraversion != null) {
            String desc = extraversion.normalized() >= 60.0
                    ? "energized by social engagement and collaborative environments"
                    : (extraversion.normalized() <= 40.0 ? "recharge effectively in thoughtful, reflective individual settings"
                    : "navigate between group activities and quiet contemplation with ease");
            sb.append(String.format("Regarding social energy, you are %s. ", desc));
        }

        if (agreeableness != null && emotionalStability != null) {
            String agreeDesc = agreeableness.normalized() >= 50.0 ? "empathetic collaboration" : "direct, candid objectivity";
            String stabDesc = emotionalStability.normalized() >= 50.0 ? "steady emotional resilience" : "acute sensitivity to environmental changes";
            sb.append(String.format("Your interactions reflect %s, supported by %s.", agreeDesc, stabDesc));
        }
    }

    private void generateLevel2Summary(StringBuilder sb, Map<String, ScoredTrait> scores) {
        ScoredTrait ambiguity = scores.get("ambiguity_tolerance");
        ScoredTrait moralFlex = scores.get("moral_flexibility");
        ScoredTrait consistency = scores.get("consistency");

        String comfortLevel = "steady adaptability";
        if (ambiguity != null) {
            comfortLevel = ambiguity.normalized() >= 65.0
                    ? "high adaptability and ease"
                    : (ambiguity.normalized() <= 35.0 ? "a strong preference for clear guidelines and defined expectations" : "measured resilience");
        }

        sb.append(String.format("You handle uncertain situations with %s. ", comfortLevel));

        if (moralFlex != null) {
            String moralDesc = moralFlex.normalized() >= 60.0
                    ? "consider contextual nuance and holistic outcomes"
                    : (moralFlex.normalized() <= 40.0 ? "anchor firmly to core foundational principles and consistent ethical standards"
                    : "weigh ethical guidelines alongside real-world contextual consequences");
            sb.append(String.format("When it comes to complex moral scenarios, you %s. ", moralDesc));
        }

        if (consistency != null && consistency.normalized() >= 70.0) {
            sb.append("Your responses reflect strong internal coherence across multi-faceted dilemmas.");
        }
    }

    private void generateLevel3Summary(StringBuilder sb, Map<String, ScoredTrait> scores) {
        // Does NOT reveal clinical/stigmatizing temperament labels, but gentle behavioral descriptions
        ScoredTrait emotionalControl = scores.get("emotional_control");
        ScoredTrait lowImpulsiveness = scores.get("impulsiveness"); // inverted so high = calm/control
        ScoredTrait clarity = scores.get("clarity_of_thinking");

        double ec = emotionalControl != null ? emotionalControl.normalized() : 50.0;
        double li = lowImpulsiveness != null ? lowImpulsiveness.normalized() : 50.0;
        double cl = clarity != null ? clarity.normalized() : 50.0;

        sb.append("Under pressure, you tend to ");

        if (ec >= 60.0 && li >= 60.0) {
            sb.append("pause and maintain inner composure, allowing you to respond with deliberate care and perspective. ");
        } else if (ec < 40.0 && li < 40.0 && cl >= 55.0) {
            sb.append("channel intense emotional urgency into swift, decisive problem-solving. ");
        } else if (ec < 40.0 && li < 40.0 && cl < 40.0) {
            sb.append("experience sensory and emotional surges that benefit from momentary pauses and grounding routines. ");
        } else {
            sb.append("balance spontaneous intuition with steady self-reflection, adjusting your responses as events unfold. ");
        }

        sb.append("This is a natural tendency, not a fixed trait.");
    }

    private String categorizeTendency(double normalizedScore) {
        if (normalizedScore >= 65.0) {
            return "high";
        } else if (normalizedScore <= 35.0) {
            return "low";
        } else {
            return "balanced";
        }
    }
}
