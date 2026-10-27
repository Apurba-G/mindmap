package com.mindmap.auth.repository;

import com.mindmap.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LoginAttempt} entity.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Counts login attempts for a specific user after a given timestamp.
     *
     * @param userId the user ID
     * @param attemptedAt threshold timestamp
     * @return count of attempts
     */
    long countByUserIdAndAttemptedAtAfter(String userId, Instant attemptedAt);

    /**
     * Counts login attempts for a specific IP address after a given timestamp.
     *
     * @param ipAddress the client IP address
     * @param attemptedAt threshold timestamp
     * @return count of attempts
     */
    long countByIpAddressAndAttemptedAtAfter(String ipAddress, Instant attemptedAt);
}
