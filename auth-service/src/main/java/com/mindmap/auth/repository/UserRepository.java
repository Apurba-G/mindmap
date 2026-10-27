package com.mindmap.auth.repository;

import com.mindmap.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their lowercase identifier.
     *
     * @param userIdLower the lowercase username
     * @return Optional containing the User if found
     */
    Optional<User> findByUserIdLower(String userIdLower);

    /**
     * Checks if a user exists with the given lowercase identifier.
     *
     * @param userIdLower the lowercase username
     * @return true if user exists, false otherwise
     */
    boolean existsByUserIdLower(String userIdLower);

    /**
     * Finds a user by their case-sensitive identifier.
     *
     * @param userId the username
     * @return Optional containing the User if found
     */
    Optional<User> findByUserId(String userId);

    /**
     * Fetches all lowercase user IDs for Bloom filter reconstruction.
     *
     * @return list of lowercase usernames
     */
    @Query("SELECT u.userIdLower FROM User u WHERE u.userIdLower IS NOT NULL")
    List<String> findAllUserIdLower();

    /**
     * Finds users by active state with pagination.
     *
     * @param isActive active status
     * @param pageable pagination details
     * @return page of users
     */
    Page<User> findByIsActive(boolean isActive, Pageable pageable);
}
