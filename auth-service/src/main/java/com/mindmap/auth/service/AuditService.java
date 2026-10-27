package com.mindmap.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmap.auth.entity.AuditLogEntry;
import com.mindmap.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for asynchronously recording audit events into auth_schema.audit_log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Asynchronously records an audit event into the database.
     *
     * @param actorId primary ID of the acting entity
     * @param actorUserId username of the acting entity
     * @param action the security/business action name
     * @param resourceType category of affected resource
     * @param resourceId specific identifier of affected resource
     * @param metadata key-value metadata to store as JSON
     * @param ipAddress client IP address
     */
    @Async("auditExecutor")
    public void log(
            UUID actorId,
            String actorUserId,
            String action,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata,
            String ipAddress
    ) {
        try {
            String metadataJson = null;
            if (metadata != null && !metadata.isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(metadata);
            }

            AuditLogEntry entry = AuditLogEntry.builder()
                    .actorId(actorId)
                    .actorUserId(actorUserId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .metadata(metadataJson)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(entry);
            log.debug("Audit logged: action={}, actor={}, resource={}:{}", action, actorUserId, resourceType, resourceId);
        } catch (Exception e) {
            log.error("Failed to record audit log entry for action: {}", action, e);
        }
    }
}
