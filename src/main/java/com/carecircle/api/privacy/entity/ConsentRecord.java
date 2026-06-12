package com.carecircle.api.privacy.entity;

import com.carecircle.api.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User acceptance for a specific version of a legal document or consent.
 *
 * <p>Records are never deleted by normal product flows. Revocation is tracked
 * with {@code revokedAt} so CareCircle keeps a minimal audit trail.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "consent_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentRecord {

    /**
     * Consent record identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * User who accepted the document.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Versioned legal document that was accepted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_document_id", nullable = false)
    private LegalDocument legalDocument;

    /**
     * Denormalized consent type used for simple filtering and audit reports.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 64)
    private LegalDocumentType consentType;

    /**
     * Acceptance timestamp.
     */
    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    /**
     * Revocation timestamp. Null means the consent is currently active.
     */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    /**
     * Request IP address at acceptance time when available.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Request user-agent at acceptance time when available.
     */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * Entity creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Creates an active consent record for a user and legal document.
     *
     * @param user accepting user.
     * @param legalDocument accepted legal document.
     */
    public ConsentRecord(User user, LegalDocument legalDocument) {
        this.user = user;
        this.legalDocument = legalDocument;
        this.consentType = legalDocument.getDocumentType();
        this.acceptedAt = OffsetDateTime.now();
    }

    /**
     * Ensures timestamps are present before insert.
     */
    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (acceptedAt == null) {
            acceptedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
