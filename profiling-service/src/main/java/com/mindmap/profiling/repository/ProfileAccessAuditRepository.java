package com.mindmap.profiling.repository;

import com.mindmap.profiling.entity.ProfileAccessAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProfileAccessAudit}.
 */
@Repository
public interface ProfileAccessAuditRepository extends JpaRepository<ProfileAccessAudit, UUID> {

    /**
     * Retrieves audit entries for actions taken by a specific administrator.
     *
     * @param adminUserId the administrator's user ID
     * @param pageable    pagination information
     * @return page of audit records
     */
    Page<ProfileAccessAudit> findByAdminUserId(UUID adminUserId, Pageable pageable);

    /**
     * Retrieves audit entries recorded when a target user's profile was accessed.
     *
     * @param targetUserId the target user's ID
     * @param pageable     pagination information
     * @return page of audit records
     */
    Page<ProfileAccessAudit> findByTargetUserId(UUID targetUserId, Pageable pageable);
}
