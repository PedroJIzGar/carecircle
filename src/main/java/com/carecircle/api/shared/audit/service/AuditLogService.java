package com.carecircle.api.shared.audit.service;

import com.carecircle.api.shared.audit.entity.AuditAction;
import com.carecircle.api.shared.audit.entity.AuditEntityType;
import com.carecircle.api.shared.audit.entity.AuditLog;
import com.carecircle.api.shared.audit.repository.AuditLogRepository;
import com.carecircle.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Writes minimal audit entries for sensitive MVP backend actions.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Records an auditable backend action.
     *
     * <p>The metadata map must stay small and must not contain private notes,
     * medical content, free-text snapshots, request bodies, or secrets.</p>
     *
     * @param actorUser authenticated user who performed the action.
     * @param action machine-readable action.
     * @param entityType affected entity type.
     * @param entityId affected entity identifier.
     * @param metadata non-sensitive operational metadata.
     */
    @Transactional
    public void record(
            User actorUser,
            AuditAction action,
            AuditEntityType entityType,
            UUID entityId,
            Map<String, Object> metadata
    ) {
        auditLogRepository.save(new AuditLog(
                actorUser,
                action,
                entityType,
                entityId,
                sanitizeMetadata(metadata)
        ));
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(metadata);
    }
}
