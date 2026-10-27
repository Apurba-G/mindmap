package com.mindmap.assessment.service;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.entity.SessionStatus;
import com.mindmap.assessment.exception.LevelLockedException;
import com.mindmap.assessment.repository.AssessmentSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing psychological assessment level unlock requirements and progression logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LevelUnlockService {

    private final AssessmentSessionRepository sessionRepository;

    /**
     * Determines whether the given assessment level is currently unlocked for the user.
     * Level 1: always unlocked
     * Level 2: unlocked if Level 1 session is COMPLETED
     * Level 3: unlocked if Level 2 session is COMPLETED
     *
     * @param userId the user's UUID
     * @param level  the target assessment level
     * @return true if unlocked, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isLevelUnlocked(UUID userId, AssessmentLevel level) {
        if (level == AssessmentLevel.LEVEL_1) {
            return true;
        }

        AssessmentLevel previousLevel = level.getPreviousLevel();
        if (previousLevel == null) {
            return true;
        }

        List<AssessmentSession> previousSessions = sessionRepository.findByUserIdAndLevel(userId, previousLevel);
        return previousSessions.stream()
                .anyMatch(session -> session.getStatus() == SessionStatus.COMPLETED);
    }

    /**
     * Computes the current active level for the user (the highest unlocked level that is not completed).
     *
     * @param userId the user's UUID
     * @return the current AssessmentLevel
     */
    @Transactional(readOnly = true)
    public AssessmentLevel getCurrentLevel(UUID userId) {
        boolean level1Completed = isLevelCompleted(userId, AssessmentLevel.LEVEL_1);
        if (!level1Completed) {
            return AssessmentLevel.LEVEL_1;
        }

        boolean level2Completed = isLevelCompleted(userId, AssessmentLevel.LEVEL_2);
        if (!level2Completed) {
            return AssessmentLevel.LEVEL_2;
        }

        return AssessmentLevel.LEVEL_3;
    }

    /**
     * Validates that the requested assessment level is unlocked for the user.
     *
     * @param userId the user's UUID
     * @param level  the target assessment level
     * @throws LevelLockedException if level is not unlocked
     */
    @Transactional(readOnly = true)
    public void validateLevelUnlocked(UUID userId, AssessmentLevel level) {
        if (!isLevelUnlocked(userId, level)) {
            AssessmentLevel prev = level.getPreviousLevel();
            throw new LevelLockedException("Assessment " + level + " is locked. You must complete " + prev + " first.");
        }
    }

    /**
     * Checks if a level has been completed by the user.
     */
    @Transactional(readOnly = true)
    public boolean isLevelCompleted(UUID userId, AssessmentLevel level) {
        List<AssessmentSession> sessions = sessionRepository.findByUserIdAndLevel(userId, level);
        return sessions.stream().anyMatch(s -> s.getStatus() == SessionStatus.COMPLETED);
    }
}
