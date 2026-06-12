package com.carecircle.api.privacy.repository;

import com.carecircle.api.privacy.entity.AuditLog;
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
}
