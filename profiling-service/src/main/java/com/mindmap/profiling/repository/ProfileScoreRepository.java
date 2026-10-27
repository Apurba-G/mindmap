package com.mindmap.profiling.repository;

import com.mindmap.profiling.entity.ProfileLevel;
import com.mindmap.profiling.entity.ProfileScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProfileScore}.
 */
@Repository
public interface ProfileScoreRepository extends JpaRepository<ProfileScore, UUID> {

    /**
     * Finds all scores associated with a profile ID and specific assessment level.
     *
     * @param profileId the UserProfile UUID
     * @param level     the assessment level
     * @return list of profile scores
     */
    List<ProfileScore> findByProfileIdAndLevel(UUID profileId, ProfileLevel level);

    /**
     * Finds all scores associated with a profile ID.
     *
     * @param profileId the UserProfile UUID
     * @return list of all scores for the profile
     */
    List<ProfileScore> findByProfileId(UUID profileId);

    /**
     * Finds a single trait score for a profile and level.
     *
     * @param profileId the profile UUID
     * @param level the assessment level
     * @param traitKey the trait key
     * @return optional containing the score
     */
    Optional<ProfileScore> findByProfileIdAndLevelAndTraitKey(UUID profileId, ProfileLevel level, String traitKey);

    /**
     * Deletes all scores for a given profile and level.
     *
     * @param profileId the UserProfile UUID
     * @param level the assessment level
     */
    @Modifying
    @Query("DELETE FROM ProfileScore s WHERE s.profile.id = :profileId AND s.level = :level")
    void deleteByProfileIdAndLevel(@Param("profileId") UUID profileId, @Param("level") ProfileLevel level);
}
