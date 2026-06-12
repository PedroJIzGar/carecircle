package com.carecircle.api.privacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Versioned legal or consent document presented to users.
 *
 * <p>The MVP stores document metadata and version identity. The actual legal
 * content can live outside the API through {@code contentUrl}, which avoids
 * turning this service into a legal CMS.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "legal_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalDocument {

    /**
     * Legal document identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Functional type of the document.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 64)
    private LegalDocumentType documentType;

    /**
     * Human-readable immutable version label.
     */
    @Column(name = "version", nullable = false, length = 40)
    private String version;

    /**
     * Display title for API clients.
     */
    @Column(name = "title", nullable = false, length = 160)
    private String title;

    /**
     * Optional URL where the complete legal text is published.
     */
    @Column(name = "content_url", length = 1024)
    private String contentUrl;

    /**
     * Optional checksum for the published document content.
     */
    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    /**
     * Whether this version is currently offered for acceptance.
     */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    /**
     * Publication timestamp for this document version.
     */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /**
     * Entity creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last metadata update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Creates a legal document metadata record.
     *
     * @param documentType legal document type.
     * @param version immutable version label.
     * @param title display title.
     */
    public LegalDocument(LegalDocumentType documentType, String version, String title) {
        this.documentType = documentType;
        this.version = version;
        this.title = title;
        this.active = true;
    }

    /**
     * Ensures timestamps are present before insert.
     */
    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Updates the modification timestamp before update.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
