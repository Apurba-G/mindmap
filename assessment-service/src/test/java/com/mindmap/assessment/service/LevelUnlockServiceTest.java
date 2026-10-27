package com.mindmap.assessment.service;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentSession;
import com.mindmap.assessment.entity.SessionStatus;
import com.mindmap.assessment.exception.LevelLockedException;
import com.mindmap.assessment.repository.AssessmentSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LevelUnlockServiceTest {

    @Mock
    private AssessmentSessionRepository sessionRepository;

    @InjectMocks
    private LevelUnlockService levelUnlockService;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Level 1 should always be unlocked")
    void level1ShouldAlwaysBeUnlocked() {
        boolean unlocked = levelUnlockService.isLevelUnlocked(userId, AssessmentLevel.LEVEL_1);
        assertTrue(unlocked);
    }

    @Test
    @DisplayName("Level 2 should be locked if Level 1 is not completed")
    void level2ShouldBeLockedIfLevel1NotCompleted() {
        when(sessionRepository.findByUserIdAndLevel(userId, AssessmentLevel.LEVEL_1))
                .thenReturn(List.of());

        boolean unlocked = levelUnlockService.isLevelUnlocked(userId, AssessmentLevel.LEVEL_2);
        assertFalse(unlocked);
        assertThrows(LevelLockedException.class, () -> levelUnlockService.validateLevelUnlocked(userId, AssessmentLevel.LEVEL_2));
    }

    @Test
    @DisplayName("Level 2 should be unlocked if Level 1 session is completed")
    void level2ShouldBeUnlockedIfLevel1Completed() {
        AssessmentSession completedL1 = AssessmentSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .level(AssessmentLevel.LEVEL_1)
                .status(SessionStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findByUserIdAndLevel(userId, AssessmentLevel.LEVEL_1))
                .thenReturn(List.of(completedL1));

        boolean unlocked = levelUnlockService.isLevelUnlocked(userId, AssessmentLevel.LEVEL_2);
        assertTrue(unlocked);
    }

    @Test
    @DisplayName("Current level calculation returns correct progression")
    void shouldReturnCurrentProgressionLevel() {
        // When Level 1 is not completed, current is Level 1
        when(sessionRepository.findByUserIdAndLevel(userId, AssessmentLevel.LEVEL_1))
                .thenReturn(List.of());
        assertEquals(AssessmentLevel.LEVEL_1, levelUnlockService.getCurrentLevel(userId));

        // When Level 1 is completed, current is Level 2
        AssessmentSession completedL1 = AssessmentSession.builder()
                .userId(userId)
                .level(AssessmentLevel.LEVEL_1)
                .status(SessionStatus.COMPLETED)
                .build();
        when(sessionRepository.findByUserIdAndLevel(userId, AssessmentLevel.LEVEL_1))
                .thenReturn(List.of(completedL1));
        when(sessionRepository.findByUserIdAndLevel(userId, AssessmentLevel.LEVEL_2))
                .thenReturn(List.of());
        assertEquals(AssessmentLevel.LEVEL_2, levelUnlockService.getCurrentLevel(userId));
    }
}
