package com.carecircle.api.privacy.dto;

import com.carecircle.api.privacy.entity.LegalDocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a legal document version.
 *
 * @param id legal document identifier.
 * @param documentType document type.
 * @param version immutable version label.
 * @param title display title.
 * @param contentUrl optional URL with full content.
 * @param contentSha256 optional content checksum.
 * @param active whether this version is currently active.
 * @param publishedAt publication timestamp.
 * @param createdAt creation timestamp.
 * @param updatedAt last update timestamp.
 */
public record LegalDocumentResponse(
        UUID id,
        LegalDocumentType documentType,
        String version,
        String title,
        String contentUrl,
        String contentSha256,
        boolean active,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
