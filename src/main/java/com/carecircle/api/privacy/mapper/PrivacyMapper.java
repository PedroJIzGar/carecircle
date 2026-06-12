package com.carecircle.api.privacy.mapper;

import com.carecircle.api.privacy.dto.ConsentRecordResponse;
import com.carecircle.api.privacy.dto.LegalDocumentResponse;
import com.carecircle.api.privacy.dto.PrivacyDocumentStatusResponse;
import com.carecircle.api.privacy.entity.ConsentRecord;
import com.carecircle.api.privacy.entity.LegalDocument;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import org.springframework.stereotype.Component;

/**
 * Maps privacy entities into API DTOs.
 */
@Component
public class PrivacyMapper {

    /**
     * Converts a legal document entity into an API response.
     *
     * @param document legal document entity.
     * @return legal document response.
     */
    public LegalDocumentResponse toLegalDocumentResponse(LegalDocument document) {
        return new LegalDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getVersion(),
                document.getTitle(),
                document.getContentUrl(),
                document.getContentSha256(),
                document.isActive(),
                document.getPublishedAt(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    /**
     * Converts a consent record into an API response.
     *
     * @param consentRecord consent record entity.
     * @return consent record response.
     */
    public ConsentRecordResponse toConsentRecordResponse(ConsentRecord consentRecord) {
        LegalDocument document = consentRecord.getLegalDocument();
        return new ConsentRecordResponse(
                consentRecord.getId(),
                consentRecord.getUser().getId(),
                document.getId(),
                document.getDocumentType(),
                document.getVersion(),
                document.getTitle(),
                consentRecord.getConsentType(),
                consentRecord.getAcceptedAt(),
                consentRecord.getRevokedAt(),
                consentRecord.getCreatedAt()
        );
    }

    /**
     * Builds the current user's acceptance status for an active document.
     *
     * @param document active legal document.
     * @param consentRecord active consent record for the document, if present.
     * @return privacy document status response.
     */
    public PrivacyDocumentStatusResponse toPrivacyDocumentStatusResponse(
            LegalDocument document,
            ConsentRecord consentRecord
    ) {
        boolean accepted = consentRecord != null;
        return new PrivacyDocumentStatusResponse(
                document.getId(),
                document.getDocumentType(),
                document.getVersion(),
                document.getTitle(),
                isRequired(document.getDocumentType()),
                accepted,
                accepted ? consentRecord.getId() : null,
                accepted ? consentRecord.getAcceptedAt() : null,
                accepted ? consentRecord.getRevokedAt() : null
        );
    }

    private boolean isRequired(LegalDocumentType documentType) {
        return switch (documentType) {
            case TERMS_OF_SERVICE, PRIVACY_POLICY, MEDICAL_DISCLAIMER -> true;
            case COMPANION_CONSENT, COMPANION_DATA_SHARING -> false;
        };
    }
}
