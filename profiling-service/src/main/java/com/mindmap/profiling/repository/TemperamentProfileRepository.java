package com.mindmap.profiling.repository;

import com.mindmap.profiling.entity.TemperamentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TemperamentProfile}.
 */
@Repository
public interface TemperamentProfileRepository extends JpaRepository<TemperamentProfile, UUID> {

    /**
     * Finds the temperament profile for a given user profile.
     *
     * @param profileId the UserProfile UUID
     * @return an optional containing the TemperamentProfile if present
     */
    Optional<TemperamentProfile> findByProfileId(UUID profileId);

    /**
     * Deletes any existing temperament profile for a given user profile.
     *
     * @param profileId the UserProfile UUID
     */
    @Modifying
    @Query("DELETE FROM TemperamentProfile t WHERE t.profile.id = :profileId")
    void deleteByProfileId(@Param("profileId") UUID profileId);
}
