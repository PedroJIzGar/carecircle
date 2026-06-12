package com.carecircle.api.shared.audit.repository;

import com.carecircle.api.shared.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence access for minimal audit logs.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Returns audit entries for an action ordered from newest to oldest.
     *
     * @param action machine-readable action.
     * @return matching audit logs.
     */
    List<AuditLog> findByActionOrderByOccurredAtDesc(String action);

    /**
     * Returns audit entries for an entity ordered from newest to oldest.
     *
     * @param entityType audited entity type.
     * @param entityId audited entity identifier.
     * @return matching audit logs.
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, UUID entityId);
}
