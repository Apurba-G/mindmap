package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.AnswerData;
import com.mindmap.profiling.dto.AssessmentCompletedEvent;
import com.mindmap.profiling.dto.ConfidenceResult;
import com.mindmap.profiling.dto.ScoredTrait;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.UserProfile;
import com.mindmap.profiling.repository.ProfileScoreRepository;
import com.mindmap.profiling.repository.ScoringRuleRepository;
import com.mindmap.profiling.repository.TemperamentProfileRepository;
import com.mindmap.profiling.repository.UserProfileRepository;
import com.mindmap.profiling.repository.UserSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private ProfileScoreRepository profileScoreRepository;
    @Mock
    private TemperamentProfileRepository temperamentProfileRepository;
    @Mock
    private ScoringRuleRepository scoringRuleRepository;
    @Mock
    private UserSummaryRepository userSummaryRepository;
    @Mock
    private ScoringEngine scoringEngine;
    @Mock
    private ConfidenceCalculator confidenceCalculator;
    @Mock
    private SummaryGenerator summaryGenerator;
    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .highestLevel(ProfileLevel.LEVEL_1)
                .overallConfidence(90.0)
                .isComplete(false)
                .build();
    }

    @Test
    @DisplayName("Successfully processes assessment completed event for Level 1")
    void testProcessAssessmentCompletedLevel1() {
        UUID sessionId = UUID.randomUUID();
        List<AnswerData> answers = List.of(
                new AnswerData(UUID.randomUUID(), UUID.randomUUID(), "openness", 5.0, 2000, false, false)
        );
        AssessmentCompletedEvent event = new AssessmentCompletedEvent(
                userId, "LEVEL_1", sessionId, answers, Instant.now()
        );

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(scoringRuleRepository.findByLevelAndIsActiveTrue(ProfileLevel.LEVEL_1)).thenReturn(Collections.emptyList());

        ConfidenceResult confidenceResult = new ConfidenceResult(95.0, 0, 0, false, Collections.emptyList());
        when(confidenceCalculator.calculateConfidence(eq(answers), any())).thenReturn(confidenceResult);

        Map<String, ScoredTrait> scoredTraits = Map.of(
                "openness", new ScoredTrait(5.0, 50.0, 95.0)
        );
        when(scoringEngine.scoreLevel1(eq(answers), any(), eq(confidenceResult))).thenReturn(scoredTraits);
        when(summaryGenerator.generateSummary(eq(ProfileLevel.LEVEL_1), eq(scoredTraits))).thenReturn("Summary text");

        profileService.processAssessmentCompleted(event);

        verify(profileScoreRepository).deleteByProfileIdAndLevel(profile.getId(), ProfileLevel.LEVEL_1);
        verify(profileScoreRepository).save(any());
        verify(userSummaryRepository).save(any());
        verify(userProfileRepository).save(profile);
        verify(kafkaProducerService).publishProfileGenerated(any());
    }
}
