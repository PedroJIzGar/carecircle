package com.carecircle.api.shared.audit.entity;

import com.carecircle.api.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal audit entry for privacy-sensitive backend actions.
 *
 * <p>Metadata must stay small and must not contain snapshots of private family
 * or health-related data. Store identifiers and operational context only.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    /**
     * Audit log identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * User who triggered the audited action, if known.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    /**
     * Machine-readable action name.
     */
    @Column(name = "action", nullable = false, length = 80)
    private String action;

    /**
     * Type of entity affected by the action.
     */
    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    /**
     * Identifier of the affected entity when available.
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Small JSON object with non-sensitive operational metadata.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    /**
     * Timestamp when the action occurred.
     */
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    /**
     * Creates an audit log entry.
     *
     * @param actorUser user who performed the action.
     * @param action machine-readable action.
     * @param entityType affected entity type.
     * @param entityId affected entity identifier.
     * @param metadata non-sensitive metadata.
     */
    public AuditLog(
            User actorUser,
            AuditAction action,
            AuditEntityType entityType,
            UUID entityId,
            Map<String, Object> metadata
    ) {
        this.actorUser = actorUser;
        this.action = action.name();
        this.entityType = entityType.name();
        this.entityId = entityId;
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    /**
     * Ensures the audit timestamp is present before insert.
     */
    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = OffsetDateTime.now();
        }
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
    }
}
