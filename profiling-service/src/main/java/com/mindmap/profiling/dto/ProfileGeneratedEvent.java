package com.mindmap.profiling.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Kafka event published to topic 'profile.generated' when a profile level calculation completes.
 *
 * @param userId    the user UUID
 * @param level     the assessment level calculated
 * @param profileId the UserProfile UUID
 */
public record ProfileGeneratedEvent(
        @NotNull UUID userId,
        @NotNull String level,
        @NotNull UUID profileId
) {
}
