package com.carecircle.api.privacy.dto;

import com.carecircle.api.privacy.entity.LegalDocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a user's consent record.
 *
 * @param id consent record identifier.
 * @param userId accepting user identifier.
 * @param legalDocumentId accepted legal document identifier.
 * @param documentType legal document type.
 * @param version accepted version.
 * @param title accepted document title.
 * @param consentType denormalized consent type.
 * @param acceptedAt acceptance timestamp.
 * @param revokedAt revocation timestamp, if revoked.
 * @param createdAt record creation timestamp.
 */
public record ConsentRecordResponse(
        UUID id,
        UUID userId,
        UUID legalDocumentId,
        LegalDocumentType documentType,
        String version,
        String title,
        LegalDocumentType consentType,
        OffsetDateTime acceptedAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt
) {
}
