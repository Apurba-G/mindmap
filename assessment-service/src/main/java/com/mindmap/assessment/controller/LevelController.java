package com.mindmap.assessment.controller;

import com.mindmap.assessment.dto.LevelStatusItem;
import com.mindmap.assessment.dto.LevelStatusResponse;
import com.mindmap.assessment.dto.ProgressDto;
import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.repository.AssessmentSessionRepository;
import com.mindmap.assessment.repository.QuestionRepository;
import com.mindmap.assessment.repository.UserAnswerRepository;
import com.mindmap.assessment.security.SecurityUtils;
import com.mindmap.assessment.service.LevelUnlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for retrieving assessment level progression and unlock statuses.
 */
@Slf4j
@RestController
@RequestMapping("/levels")
@RequiredArgsConstructor
public class LevelController {

    private final LevelUnlockService levelUnlockService;
    private final AssessmentSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;

    /**
     * Returns the unlock and completion statuses for all assessment levels for the current user.
     */
    @GetMapping("/status")
    public ResponseEntity<LevelStatusResponse> getLevelStatus() {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("REST: get level statuses for user {}", userId);

        Map<AssessmentLevel, LevelStatusItem> levelMap = new EnumMap<>(AssessmentLevel.class);

        for (AssessmentLevel level : AssessmentLevel.values()) {
            boolean unlocked = levelUnlockService.isLevelUnlocked(userId, level);
            Optional<AssessmentSession> latestSession = sessionRepository
                    .findFirstByUserIdAndLevelOrderByStartedAtDesc(userId, level);

            long totalQuestions = questionRepository.countByLevelAndIsActiveTrue(level);

            LevelStatusItem item;
            if (latestSession.isPresent()) {
                AssessmentSession session = latestSession.get();
                long answered = userAnswerRepository.countBySessionId(session.getId());
                ProgressDto progress = ProgressDto.of((int) answered, (int) totalQuestions);
                item = new LevelStatusItem(unlocked, session.getStatus(), progress, session.getCompletedAt());
            } else {
                ProgressDto progress = ProgressDto.of(0, (int) totalQuestions);
                item = new LevelStatusItem(unlocked, null, progress, null);
            }

            levelMap.put(level, item);
        }

        AssessmentLevel currentLevel = levelUnlockService.getCurrentLevel(userId);
        return ResponseEntity.ok(new LevelStatusResponse(levelMap, currentLevel));
    }
}
