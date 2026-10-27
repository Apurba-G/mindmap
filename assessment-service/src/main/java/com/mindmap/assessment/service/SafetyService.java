package com.mindmap.assessment.service;

import com.mindmap.assessment.dto.SafetyAlert;
import com.mindmap.assessment.entity.AnswerOption;
import com.mindmap.assessment.entity.SafetyFlag;
import com.mindmap.assessment.repository.SafetyFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing psychological safety detection and crisis alert notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyService {

    private final SafetyFlagRepository safetyFlagRepository;

    @Value("${app.safety.alert-message:We noticed your response may reflect some difficult feelings. You're not alone. If you need support, please reach out to the crisis resources listed below. This information has been noted confidentially.}")
    private String alertMessage;

    @Value("${app.safety.helpline-numbers:US/Canada: 988 (Suicide & Crisis Lifeline),UK: 111 (NHS Mental Health) or 116 123 (Samaritans),International: https://findahelpline.com,Crisis Text Line: Text HOME to 741741}")
    private List<String> helplineNumbers;

    /**
     * Inspects the selected answer option for safety/distress flags.
     * If flagged, records a confidential SafetyFlag in the database and generates a SafetyAlert.
     *
     * @param userId     the answering user
     * @param sessionId  the current assessment session
     * @param questionId the question answered
     * @param selected   the chosen option
     * @return Optional containing SafetyAlert if flagged, empty otherwise
     */
    @Transactional
    public Optional<SafetyAlert> checkForSafetyFlags(UUID userId, UUID sessionId, UUID questionId, AnswerOption selected) {
        if (selected == null || !selected.isSafetyFlag()) {
            return Optional.empty();
        }

        log.warn("Safety flag triggered by user {} in session {} for question {}", userId, sessionId, questionId);

        SafetyFlag safetyFlag = SafetyFlag.builder()
                .userId(userId)
                .sessionId(sessionId)
                .questionId(questionId)
                .flagType("PSYCHOLOGICAL_DISTRESS")
                .details(String.format("Option [%s]: %s (Trait: %s)",
                        selected.getOptionLabel(),
                        selected.getOptionText(),
                        selected.getTraitKey()))
                .reviewed(false)
                .build();

        safetyFlagRepository.save(safetyFlag);

        SafetyAlert alert = new SafetyAlert(
                "SUPPORT",
                alertMessage,
                helplineNumbers
        );

        return Optional.of(alert);
    }
}
