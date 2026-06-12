package com.carecircle.api.privacy.dto;

import com.carecircle.api.privacy.entity.LegalDocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Acceptance status for the current active version of one legal document.
 *
 * @param legalDocumentId current legal document identifier.
 * @param documentType document type.
 * @param version current version.
 * @param title display title.
 * @param required whether CareCircle treats this document as required for account use.
 * @param accepted whether the current user has an active acceptance for this version.
 * @param consentRecordId active consent record identifier when accepted.
 * @param acceptedAt active acceptance timestamp when accepted.
 * @param revokedAt revocation timestamp for active status responses; normally null.
 */
public record PrivacyDocumentStatusResponse(
        UUID legalDocumentId,
        LegalDocumentType documentType,
        String version,
        String title,
        boolean required,
        boolean accepted,
        UUID consentRecordId,
        OffsetDateTime acceptedAt,
        OffsetDateTime revokedAt
) {
}
