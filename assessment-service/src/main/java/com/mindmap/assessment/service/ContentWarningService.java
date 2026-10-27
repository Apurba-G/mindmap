package com.mindmap.assessment.service;

import com.mindmap.assessment.entity.AssessmentLevel;
import com.mindmap.assessment.entity.AssessmentPart;
import com.mindmap.assessment.entity.ContentWarningAck;
import com.mindmap.assessment.repository.ContentWarningAckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing user consents and acknowledgments for sensitive assessment content warnings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentWarningService {

    private final ContentWarningAckRepository ackRepository;

    /**
     * Records or updates a user's acknowledgment of a content warning for a specific level and part.
     */
    @Transactional
    public ContentWarningAck acknowledgeWarning(UUID userId, AssessmentLevel level, AssessmentPart part, boolean accepted) {
        log.info("Recording content warning ack for user={}, level={}, part={}, accepted={}", userId, level, part, accepted);
        Optional<ContentWarningAck> existing = ackRepository.findByUserIdAndLevelAndPart(userId, level, part);

        ContentWarningAck ack;
        if (existing.isPresent()) {
            ack = existing.get();
            ack.setAccepted(accepted);
            ack.setAckedAt(LocalDateTime.now());
        } else {
            ack = ContentWarningAck.builder()
                    .userId(userId)
                    .level(level)
                    .part(part)
                    .accepted(accepted)
                    .build();
        }

        return ackRepository.save(ack);
    }

    /**
     * Checks if a user has accepted content warnings for a level and part.
     */
    @Transactional(readOnly = true)
    public boolean hasAcceptedWarning(UUID userId, AssessmentLevel level, AssessmentPart part) {
        return ackRepository.existsByUserIdAndLevelAndPartAndAcceptedTrue(userId, level, part);
    }
}
