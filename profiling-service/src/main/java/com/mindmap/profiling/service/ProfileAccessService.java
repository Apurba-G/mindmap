package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.FullProfileResponse;
import com.mindmap.profiling.dto.LevelScoresResponse;
import com.mindmap.profiling.dto.ScoredTrait;
import com.mindmap.profiling.dto.SummaryItemDto;
import com.mindmap.profiling.dto.TemperamentResponse;
import com.mindmap.profiling.dto.UserProfileSummaryResponse;
import com.mindmap.profiling.entity.ProfileAccessAudit;
import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.ProfileScore;
import com.mindmap.profiling.entity.SummaryVisibility;
import com.mindmap.profiling.entity.TemperamentProfile;
import com.mindmap.profiling.entity.TemperamentType;
import com.mindmap.profiling.entity.UserProfile;
import com.mindmap.profiling.entity.UserSummary;
import com.mindmap.profiling.exception.LevelNotCompletedException;
import com.mindmap.profiling.exception.ProfileNotFoundException;
import com.mindmap.profiling.repository.ProfileAccessAuditRepository;
import com.mindmap.profiling.repository.ProfileScoreRepository;
import com.mindmap.profiling.repository.TemperamentProfileRepository;
import com.mindmap.profiling.repository.UserProfileRepository;
import com.mindmap.profiling.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing profile access control, summary rendering for users,
 * and audited administrative diagnostic inspections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileAccessService {

    private final UserProfileRepository userProfileRepository;
    private final ProfileScoreRepository profileScoreRepository;
    private final TemperamentProfileRepository temperamentProfileRepository;
    private final UserSummaryRepository userSummaryRepository;
    private final ProfileAccessAuditRepository profileAccessAuditRepository;

    /**
     * User-facing inquiry returning empathetic level summaries and progress status.
     * Raw scores, diagnostic axes, and temperament labels are strictly withheld.
     *
     * @param userId target user ID
     * @return {@link UserProfileSummaryResponse}
     */
    @Transactional(readOnly = true)
    public UserProfileSummaryResponse getUserSummary(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));

        List<UserSummary> summaries = userSummaryRepository.findByProfileId(profile.getId());

        List<SummaryItemDto> visibleSummaries = summaries.stream()
                .filter(s -> s.getVisibility() != SummaryVisibility.NONE)
                .map(s -> new SummaryItemDto(s.getLevel().name(), s.getSummaryText(), s.getVisibility().name()))
                .toList();

        return new UserProfileSummaryResponse(
                Boolean.TRUE.equals(profile.getIsComplete()),
                profile.getHighestLevel() != null ? profile.getHighestLevel().name() : null,
                profile.getOverallConfidence(),
                visibleSummaries,
                UserProfileSummaryResponse.DEFAULT_DISCLAIMER
        );
    }

    /**
     * Administrative inquiry retrieving complete diagnostic profile data across all levels.
     * Every access is immutably recorded in the security audit trail.
     *
     * @param targetUserId user whose profile is inspected
     * @param adminUserId  administrator performing inspection
     * @param adminHandle  administrator handle
     * @param ipAddress    originating client IP
     * @return {@link FullProfileResponse}
     */
    @Transactional
    public FullProfileResponse getFullProfile(UUID targetUserId, UUID adminUserId, String adminHandle, String ipAddress) {
        log.info("Admin {} ({}) accessing full profile of user {} from IP {}",
                adminHandle, adminUserId, targetUserId, ipAddress);

        // 1. Immutable audit record
        recordAudit(adminUserId, adminHandle, targetUserId, "FULL_PROFILE_INSPECTION", ipAddress);

        // 2. Fetch UserProfile
        UserProfile profile = userProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ProfileNotFoundException(targetUserId));

        // 3. Assemble level scores
        LevelScoresResponse l1 = buildLevelScores(profile.getId(), ProfileLevel.LEVEL_1);
        LevelScoresResponse l2 = buildLevelScores(profile.getId(), ProfileLevel.LEVEL_2);
        LevelScoresResponse l3 = buildLevelScores(profile.getId(), ProfileLevel.LEVEL_3);

        // 4. Assemble temperament
        TemperamentResponse temperament = temperamentProfileRepository.findByProfileId(profile.getId())
                .map(t -> mapToTemperamentResponse(t, profile.getOverallConfidence()))
                .orElse(null);

        // 5. Audit safety flags (e.g. low stability, high impulsiveness, low control)
        List<String> safetyFlags = evaluateSafetyFlags(l1, l2, l3, temperament);

        return new FullProfileResponse(
                profile.getUserId(),
                "user-" + targetUserId.toString().substring(0, 8),
                Boolean.TRUE.equals(profile.getIsComplete()),
                profile.getOverallConfidence(),
                l1,
                l2,
                l3,
                temperament,
                safetyFlags,
                FullProfileResponse.ADMIN_DISCLAIMER
        );
    }

    /**
     * Administrative inquiry retrieving scores for a specific assessment level with audit logging.
     *
     * @param targetUserId target user ID
     * @param level        assessment level
     * @param adminUserId  administrator ID
     * @param adminHandle  administrator handle
     * @param ipAddress    client IP
     * @return {@link LevelScoresResponse}
     */
    @Transactional
    public LevelScoresResponse getLevelScores(UUID targetUserId, ProfileLevel level, UUID adminUserId, String adminHandle, String ipAddress) {
        recordAudit(adminUserId, adminHandle, targetUserId, "LEVEL_SCORES_" + level, ipAddress);

        UserProfile profile = userProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ProfileNotFoundException(targetUserId));

        return buildLevelScores(profile.getId(), level);
    }

    /**
     * Administrative inquiry retrieving temperament breakdown with audit logging.
     *
     * @param targetUserId target user ID
     * @param adminUserId  administrator ID
     * @param adminHandle  administrator handle
     * @param ipAddress    client IP
     * @return {@link TemperamentResponse}
     */
    @Transactional
    public TemperamentResponse getTemperament(UUID targetUserId, UUID adminUserId, String adminHandle, String ipAddress) {
        recordAudit(adminUserId, adminHandle, targetUserId, "TEMPERAMENT_INSPECTION", ipAddress);

        UserProfile profile = userProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ProfileNotFoundException(targetUserId));

        TemperamentProfile temperament = temperamentProfileRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new LevelNotCompletedException(targetUserId, ProfileLevel.LEVEL_3));

        return mapToTemperamentResponse(temperament, profile.getOverallConfidence());
    }

    /**
     * Retrieves paginated audit logs for a specific administrator or target user.
     *
     * @param adminUserId  optional filter by administrator ID
     * @param targetUserId optional filter by target user ID
     * @param pageable     pagination params
     * @return page of {@link ProfileAccessAudit}
     */
    @Transactional(readOnly = true)
    public Page<ProfileAccessAudit> getAuditLogs(UUID adminUserId, UUID targetUserId, Pageable pageable) {
        if (targetUserId != null) {
            return profileAccessAuditRepository.findByTargetUserId(targetUserId, pageable);
        } else if (adminUserId != null) {
            return profileAccessAuditRepository.findByAdminUserId(adminUserId, pageable);
        } else {
            return profileAccessAuditRepository.findAll(pageable);
        }
    }

    private void recordAudit(UUID adminUserId, String adminHandle, UUID targetUserId, String accessType, String ipAddress) {
        ProfileAccessAudit audit = ProfileAccessAudit.builder()
                .adminUserId(adminUserId)
                .adminHandle(adminHandle != null ? adminHandle : "admin")
                .targetUserId(targetUserId)
                .accessType(accessType)
                .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                .build();
        profileAccessAuditRepository.save(audit);
    }

    private LevelScoresResponse buildLevelScores(UUID profileId, ProfileLevel level) {
        List<ProfileScore> scores = profileScoreRepository.findByProfileIdAndLevel(profileId, level);
        Map<String, ScoredTrait> traitMap = new HashMap<>();

        for (ProfileScore score : scores) {
            traitMap.put(score.getTraitKey(), new ScoredTrait(score.getRawScore(), score.getNormalized(), score.getConfidence()));
        }

        return new LevelScoresResponse(level.name(), traitMap);
    }

    private TemperamentResponse mapToTemperamentResponse(TemperamentProfile t, Double overallConfidence) {
        Map<TemperamentType, Double> distribution = new HashMap<>();
        distribution.put(TemperamentType.STRONG_AND_CALM, t.getStrongCalmPct());
        distribution.put(TemperamentType.MODERATE_TEMPER, t.getModerateTemperPct());
        distribution.put(TemperamentType.LOW_CONTROL_ANGRY, t.getLowControlAngryPct());
        distribution.put(TemperamentType.LOW_CONTROL_CONFUSED, t.getLowControlConfusedPct());

        return new TemperamentResponse(
                t.getPrimaryType(),
                distribution,
                t.getExplanationText(),
                overallConfidence != null ? overallConfidence : 80.0
        );
    }

    private List<String> evaluateSafetyFlags(
            LevelScoresResponse l1,
            LevelScoresResponse l2,
            LevelScoresResponse l3,
            TemperamentResponse temperament
    ) {
        List<String> flags = Collections.emptyList();
        var mutableFlags = new java.util.ArrayList<String>();

        if (l1 != null && l1.traits() != null) {
            ScoredTrait stability = l1.traits().get("emotional_stability");
            if (stability != null && stability.normalized() < 15.0) {
                mutableFlags.add("SAFETY_ALERT: Severe emotional distress or instability indication");
            }
        }

        if (temperament != null) {
            if (temperament.primaryType() == TemperamentType.LOW_CONTROL_ANGRY &&
                    temperament.breakdownPercentages().getOrDefault(TemperamentType.LOW_CONTROL_ANGRY, 0.0) >= 60.0) {
                mutableFlags.add("CLINICAL_FLAG: Predominant low impulse control under stress");
            }
            if (temperament.primaryType() == TemperamentType.LOW_CONTROL_CONFUSED &&
                    temperament.breakdownPercentages().getOrDefault(TemperamentType.LOW_CONTROL_CONFUSED, 0.0) >= 60.0) {
                mutableFlags.add("CLINICAL_FLAG: Significant cognitive overwhelm and disorganization under stress");
            }
        }

        return mutableFlags;
    }
}
