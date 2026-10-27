package com.mindmap.auth.repository;

import com.mindmap.auth.entity.ReservedUsername;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ReservedUsername} entity.
 */
@Repository
public interface ReservedUsernameRepository extends JpaRepository<ReservedUsername, Long> {

    /**
     * Checks if a reserved username exists matching the specified word.
     *
     * @param word the reserved word to check
     * @return true if reserved, false otherwise
     */
    boolean existsByWord(String word);

    /**
     * Finds a reserved username entry by word.
     *
     * @param word the reserved word
     * @return Optional containing the ReservedUsername if found
     */
    Optional<ReservedUsername> findByWord(String word);
}
