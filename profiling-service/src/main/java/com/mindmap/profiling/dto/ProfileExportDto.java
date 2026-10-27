package com.mindmap.profiling.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data export container enabling GDPR compliance and data portability for user profiles.
 *
 * @param profileId   the internal profile UUID
 * @param userId      the user UUID
 * @param isComplete  whether profiling is complete
 * @param createdAt   profile creation timestamp
 * @param updatedAt   profile update timestamp
 * @param summaries   user narrative summaries
 */
public record ProfileExportDto(
        UUID profileId,
        UUID userId,
        boolean isComplete,
        Instant createdAt,
        Instant updatedAt,
        List<SummaryItemDto> summaries
) {
}
