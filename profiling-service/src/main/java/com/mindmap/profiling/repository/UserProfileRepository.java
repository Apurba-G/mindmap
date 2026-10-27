package com.mindmap.profiling.repository;

import com.mindmap.profiling.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserProfile}.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Finds a user profile by the user's unique identifier.
     *
     * @param userId the user's UUID
     * @return an optional containing the UserProfile if found
     */
    Optional<UserProfile> findByUserId(UUID userId);

    /**
     * Checks if a profile exists for the given user ID.
     *
     * @param userId the user's UUID
     * @return true if profile exists, false otherwise
     */
    boolean existsByUserId(UUID userId);

    /**
     * Finds a user profile with eagerly fetched scores and summaries.
     *
     * @param userId the user's UUID
     * @return an optional containing the UserProfile with related entities
     */
    @Query("SELECT p FROM UserProfile p LEFT JOIN FETCH p.scores LEFT JOIN FETCH p.summaries WHERE p.userId = :userId")
    Optional<UserProfile> findByUserIdWithDetails(@Param("userId") UUID userId);
}
