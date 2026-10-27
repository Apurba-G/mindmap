package com.mindmap.auth.repository;

import com.mindmap.auth.entity.RecoveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RecoveryAttempt} entity.
 */
@Repository
public interface RecoveryAttemptRepository extends JpaRepository<RecoveryAttempt, UUID> {

    /**
     * Counts recovery attempts for a specific user after a given timestamp.
     *
     * @param userId the user ID
     * @param attemptedAt threshold timestamp
     * @return count of attempts
     */
    long countByUserIdAndAttemptedAtAfter(String userId, Instant attemptedAt);

    /**
     * Counts recovery attempts for a specific IP address after a given timestamp.
     *
     * @param ipAddress the client IP address
     * @param attemptedAt threshold timestamp
     * @return count of attempts
     */
    long countByIpAddressAndAttemptedAtAfter(String ipAddress, Instant attemptedAt);
}
