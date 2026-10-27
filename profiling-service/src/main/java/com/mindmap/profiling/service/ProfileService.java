package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.AssessmentCompletedEvent;
import com.mindmap.profiling.dto.CompletionStatusResponse;
import com.mindmap.profiling.dto.ConfidenceResult;
import com.mindmap.profiling.dto.ProfileExportDto;
import com.mindmap.profiling.dto.ProfileGeneratedEvent;
import com.mindmap.profiling.dto.ScoredTrait;
import com.mindmap.profiling.dto.SummaryItemDto;
import com.mindmap.profiling.dto.TemperamentCalculationResult;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.ProfileScore;
import com.mindmap.profiling.entity.ScoringRule;
import com.mindmap.profiling.entity.SummaryVisibility;
import com.mindmap.profiling.entity.TemperamentProfile;
import com.mindmap.profiling.entity.UserProfile;
import com.mindmap.profiling.entity.UserSummary;
import com.mindmap.profiling.exception.ProfileNotFoundException;
import com.mindmap.profiling.repository.ProfileScoreRepository;
import com.mindmap.profiling.repository.ScoringRuleRepository;
import com.mindmap.profiling.repository.TemperamentProfileRepository;
import com.mindmap.profiling.repository.UserProfileRepository;
import com.mindmap.profiling.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing profile lifecycles, assessment ingestion, score aggregation, and reporting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ProfileScoreRepository profileScoreRepository;
    private final TemperamentProfileRepository temperamentProfileRepository;
    private final ScoringRuleRepository scoringRuleRepository;
    private final UserSummaryRepository userSummaryRepository;

    private final ScoringEngine scoringEngine;
    private final ConfidenceCalculator confidenceCalculator;
    private final SummaryGenerator summaryGenerator;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Ingests and processes an assessment completion event.
     * Executes the scoring strategy, updates profile metrics, persists narrative summaries,
     * and emits a profile generation event.
     *
     * @param event the received assessment completion event
     */
    @Transactional
    public void processAssessmentCompleted(AssessmentCompletedEvent event) {
        UUID userId = event.userId();
        ProfileLevel level = ProfileLevel.fromString(event.level());
        log.info("Processing assessment completion for userId={}, level={}, sessionId={}",
                userId, level, event.sessionId());

        // 1. Load or create UserProfile
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new UserProfile for userId={}", userId);
                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .highestLevel(level)
                            .overallConfidence(100.0)
                            .isComplete(false)
                            .build();
                    return userProfileRepository.save(newProfile);
                });

        // 2. Load applicable ScoringRules
        List<ScoringRule> rules = scoringRuleRepository.findByLevelAndIsActiveTrue(level);

        // 3. Calculate Confidence
        ConfidenceResult confidenceResult = confidenceCalculator.calculateConfidence(event.answers(), rules);

        // 4. Run ScoringEngine for appropriate level
        Map<String, ScoredTrait> scoredTraits;
        TemperamentCalculationResult temperamentResult = null;

        if (level == ProfileLevel.LEVEL_1) {
            scoredTraits = scoringEngine.scoreLevel1(event.answers(), rules, confidenceResult);
        } else if (level == ProfileLevel.LEVEL_2) {
            scoredTraits = scoringEngine.scoreLevel2(event.answers(), rules, confidenceResult);
        } else if (level == ProfileLevel.LEVEL_3) {
            temperamentResult = scoringEngine.scoreLevel3(event.answers(), rules, confidenceResult);
            scoredTraits = temperamentResult.axisScores();
        } else {
            throw new IllegalArgumentException("Unsupported profiling level: " + level);
        }

        // 5. Save ProfileScores (remove older calculations for this level if present)
        profileScoreRepository.deleteByProfileIdAndLevel(profile.getId(), level);

        for (Map.Entry<String, ScoredTrait> entry : scoredTraits.entrySet()) {
            ScoredTrait scoredTrait = entry.getValue();
            ProfileScore scoreEntity = ProfileScore.builder()
                    .profile(profile)
                    .level(level)
                    .traitKey(entry.getKey())
                    .rawScore(scoredTrait.rawScore())
                    .normalized(scoredTrait.normalized())
                    .confidence(scoredTrait.confidence())
                    .build();
            profileScoreRepository.save(scoreEntity);
        }

        // 6. If Level 3, save TemperamentProfile
        if (level == ProfileLevel.LEVEL_3 && temperamentResult != null) {
            temperamentProfileRepository.deleteByProfileId(profile.getId());

            TemperamentProfile temperamentEntity = TemperamentProfile.builder()
                    .profile(profile)
                    .strongCalmPct(temperamentResult.strongCalmPct())
                    .moderateTemperPct(temperamentResult.moderateTemperPct())
                    .lowControlAngryPct(temperamentResult.lowControlAngryPct())
                    .lowControlConfusedPct(temperamentResult.lowControlConfusedPct())
                    .primaryType(temperamentResult.primaryType())
                    .explanationText(temperamentResult.explanationText())
                    .build();
            temperamentProfileRepository.save(temperamentEntity);
        }

        // 7. Generate User Summary
        String summaryText = summaryGenerator.generateSummary(level, scoredTraits);
        userSummaryRepository.deleteByProfileIdAndLevel(profile.getId(), level);

        UserSummary userSummary = UserSummary.builder()
                .profile(profile)
                .level(level)
                .summaryText(summaryText)
                .visibility(SummaryVisibility.SHORT)
                .build();
        userSummaryRepository.save(userSummary);

        // 8. Update UserProfile metrics and completion
        updateProfileStatus(profile, level, confidenceResult.overallConfidence(), summaryText);
        userProfileRepository.save(profile);

        // 9. Publish profile.generated event
        ProfileGeneratedEvent generatedEvent = new ProfileGeneratedEvent(userId, level.name(), profile.getId());
        kafkaProducerService.publishProfileGenerated(generatedEvent);

        log.info("Successfully completed profile generation for userId={}, level={}", userId, level);
    }

    /**
     * Retrieves the completion status for a given user.
     *
     * @param userId user identifier
     * @return {@link CompletionStatusResponse}
     */
    @Transactional(readOnly = true)
    public CompletionStatusResponse getCompletionStatus(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));

        List<ProfileScore> allScores = profileScoreRepository.findByProfileId(profile.getId());
        List<String> completedLevels = allScores.stream()
                .map(s -> s.getLevel().name())
                .distinct()
                .sorted()
                .toList();

        return new CompletionStatusResponse(
                profile.getUserId(),
                Boolean.TRUE.equals(profile.getIsComplete()),
                profile.getHighestLevel() != null ? profile.getHighestLevel().name() : null,
                profile.getOverallConfidence(),
                completedLevels,
                profile.getUpdatedAt()
        );
    }

    /**
     * Exports full user profile data for portability and GDPR compliance.
     *
     * @param userId user identifier
     * @return {@link ProfileExportDto}
     */
    @Transactional(readOnly = true)
    public ProfileExportDto exportUserProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));

        List<UserSummary> summaries = userSummaryRepository.findByProfileId(profile.getId());
        List<SummaryItemDto> summaryDtos = summaries.stream()
                .map(s -> new SummaryItemDto(s.getLevel().name(), s.getSummaryText(), s.getVisibility().name()))
                .toList();

        return new ProfileExportDto(
                profile.getId(),
                profile.getUserId(),
                Boolean.TRUE.equals(profile.getIsComplete()),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                summaryDtos
        );
    }

    /**
     * Deletes all profile records for a user under GDPR "Right to be Forgotten".
     *
     * @param userId user identifier
     */
    @Transactional
    public void deleteUserProfile(UUID userId) {
        log.info("Deleting profile and all associated data for userId={}", userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));

        userProfileRepository.delete(profile);
        log.info("Successfully deleted user profile for userId={}", userId);
    }

    private void updateProfileStatus(UserProfile profile, ProfileLevel currentLevel, double currentConfidence, String summaryText) {
        // Update highestLevel if current level is higher or equal
        if (profile.getHighestLevel() == null || currentLevel.ordinal() >= profile.getHighestLevel().ordinal()) {
            profile.setHighestLevel(currentLevel);
        }

        // Recompute composite confidence as moving average
        if (profile.getOverallConfidence() == null || profile.getOverallConfidence() == 0.0) {
            profile.setOverallConfidence(currentConfidence);
        } else {
            profile.setOverallConfidence(Math.round(((profile.getOverallConfidence() + currentConfidence) / 2.0) * 100.0) / 100.0);
        }

        // Mark as complete once Level 3 has been processed
        if (currentLevel == ProfileLevel.LEVEL_3) {
            profile.setIsComplete(true);
        }

        // Encrypt/store summary snapshot
        if (summaryText != null) {
            profile.setEncryptedSummary(summaryText.getBytes(StandardCharsets.UTF_8));
        }
    }
}
