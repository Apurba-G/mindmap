package com.mindmap.auth.repository;

import com.mindmap.auth.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AuditLogEntry} entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    /**
     * Finds audit log entries triggered by a specific actor.
     *
     * @param actorId the actor's UUID
     * @param pageable pagination details
     * @return page of audit log entries
     */
    Page<AuditLogEntry> findByActorId(UUID actorId, Pageable pageable);

    /**
     * Finds audit log entries for a given resource type and ID.
     *
     * @param resourceType the target resource type
     * @param resourceId the target resource ID
     * @param pageable pagination details
     * @return page of audit log entries
     */
    Page<AuditLogEntry> findByResourceTypeAndResourceId(String resourceType, String resourceId, Pageable pageable);
}
