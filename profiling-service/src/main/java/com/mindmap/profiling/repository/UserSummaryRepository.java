package com.mindmap.profiling.repository;

import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.UserSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserSummary}.
 */
@Repository
public interface UserSummaryRepository extends JpaRepository<UserSummary, UUID> {

    /**
     * Finds the summary for a specific profile and assessment level.
     *
     * @param profileId the UserProfile UUID
     * @param level     the assessment level
     * @return an optional containing the UserSummary if present
     */
    Optional<UserSummary> findByProfileIdAndLevel(UUID profileId, ProfileLevel level);

    /**
     * Finds all summaries for a specific profile.
     *
     * @param profileId the UserProfile UUID
     * @return list of summaries
     */
    List<UserSummary> findByProfileId(UUID profileId);

    /**
     * Deletes the summary for a specific profile and assessment level.
     *
     * @param profileId the profile UUID
     * @param level     the assessment level
     */
    @Modifying
    @Query("DELETE FROM UserSummary s WHERE s.profile.id = :profileId AND s.level = :level")
    void deleteByProfileIdAndLevel(@Param("profileId") UUID profileId, @Param("level") ProfileLevel level);
}
