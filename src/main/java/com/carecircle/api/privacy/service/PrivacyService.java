package com.carecircle.api.privacy.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.privacy.dto.AcceptConsentRequest;
import com.carecircle.api.privacy.dto.ConsentRecordResponse;
import com.carecircle.api.privacy.dto.LegalDocumentResponse;
import com.carecircle.api.privacy.dto.PrivacyStatusResponse;
import com.carecircle.api.privacy.entity.ConsentRecord;
import com.carecircle.api.privacy.entity.LegalDocument;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import com.carecircle.api.privacy.mapper.PrivacyMapper;
import com.carecircle.api.privacy.repository.ConsentRecordRepository;
import com.carecircle.api.privacy.repository.LegalDocumentRepository;
import com.carecircle.api.shared.exception.ResourceConflictException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for privacy, legal document and consent workflows.
 */
@Service
@RequiredArgsConstructor
public class PrivacyService {

    private static final Comparator<LegalDocument> DOCUMENT_ORDER = Comparator
            .comparingInt((LegalDocument document) -> getDocumentOrder(document.getDocumentType()))
            .thenComparing(LegalDocument::getVersion, Comparator.nullsLast(Comparator.naturalOrder()));

    private final UserService userService;
    private final LegalDocumentRepository legalDocumentRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final AuditLogService auditLogService;
    private final PrivacyMapper privacyMapper;

    /**
     * Lists active legal documents offered by the API.
     *
     * @return active legal document responses.
     */
    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> listActiveLegalDocuments() {
        return findActiveDocuments().stream()
                .map(privacyMapper::toLegalDocumentResponse)
                .toList();
    }

    /**
     * Returns the authenticated user's acceptance status for active documents.
     *
     * @param claims normalized Supabase claims.
     * @return privacy status response.
     */
    @Transactional(readOnly = true)
    public PrivacyStatusResponse getCurrentPrivacyStatus(SupabaseUserClaims claims) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        List<LegalDocument> activeDocuments = findActiveDocuments();
        Map<UUID, ConsentRecord> activeConsentByDocumentId = getActiveConsentByDocumentId(currentUser, activeDocuments);

        return new PrivacyStatusResponse(activeDocuments.stream()
                .map(document -> privacyMapper.toPrivacyDocumentStatusResponse(
                        document,
                        activeConsentByDocumentId.get(document.getId())
                ))
                .toList());
    }

    /**
     * Accepts the current active version of a legal document type.
     *
     * <p>The operation is intentionally idempotent for the same active document.
     * Repeating the same acceptance returns the existing active record instead
     * of creating duplicates.</p>
     *
     * @param claims normalized Supabase claims.
     * @param request acceptance request.
     * @param ipAddress request IP address when available.
     * @param userAgent request user-agent when available.
     * @return active consent record response.
     */
    @Transactional
    public ConsentRecordResponse acceptConsent(
            SupabaseUserClaims claims,
            AcceptConsentRequest request,
            String ipAddress,
            String userAgent
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        LegalDocument activeDocument = getActiveDocumentOrThrow(request.documentType());

        ConsentRecord existingConsent = consentRecordRepository
                .findByUser_IdAndLegalDocument_IdAndRevokedAtIsNull(currentUser.getId(), activeDocument.getId())
                .orElse(null);
        if (existingConsent != null) {
            updateDirectUserAcceptanceTimestamp(currentUser, existingConsent.getConsentType(), existingConsent.getAcceptedAt());
            return privacyMapper.toConsentRecordResponse(existingConsent);
        }

        OffsetDateTime now = OffsetDateTime.now();
        revokeOlderActiveConsentsForType(currentUser, activeDocument.getDocumentType(), activeDocument.getId(), now);

        ConsentRecord consentRecord = new ConsentRecord(currentUser, activeDocument);
        consentRecord.setAcceptedAt(now);
        consentRecord.setIpAddress(truncate(ipAddress, 45));
        consentRecord.setUserAgent(truncate(userAgent, 512));

        ConsentRecord savedConsentRecord = consentRecordRepository.save(consentRecord);
        updateDirectUserAcceptanceTimestamp(currentUser, savedConsentRecord.getConsentType(), savedConsentRecord.getAcceptedAt());
        auditLogService.recordConsentAccepted(currentUser, savedConsentRecord);

        return privacyMapper.toConsentRecordResponse(savedConsentRecord);
    }

    /**
     * Revokes an optional consent owned by the authenticated user.
     *
     * <p>Required account legal acceptances are not revoked through this MVP
     * endpoint because account deactivation/deletion is a separate workflow.</p>
     *
     * @param claims normalized Supabase claims.
     * @param consentRecordId consent record identifier.
     * @return revoked consent record response.
     */
    @Transactional
    public ConsentRecordResponse revokeConsent(SupabaseUserClaims claims, UUID consentRecordId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        ConsentRecord consentRecord = consentRecordRepository.findByIdAndUser_Id(consentRecordId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Consent record not found."));

        if (consentRecord.getRevokedAt() != null) {
            throw new ResourceConflictException("Consent record is already revoked.");
        }
        if (isRequiredLegalAcceptance(consentRecord.getConsentType())) {
            throw new ResourceConflictException("Required legal documents cannot be revoked through this endpoint.");
        }

        consentRecord.setRevokedAt(OffsetDateTime.now());
        auditLogService.recordConsentRevoked(currentUser, consentRecord);

        return privacyMapper.toConsentRecordResponse(consentRecord);
    }

    private List<LegalDocument> findActiveDocuments() {
        return legalDocumentRepository.findByActiveTrue().stream()
                .sorted(DOCUMENT_ORDER)
                .toList();
    }

    private LegalDocument getActiveDocumentOrThrow(LegalDocumentType documentType) {
        return legalDocumentRepository
                .findFirstByDocumentTypeAndActiveTrueOrderByPublishedAtDescCreatedAtDesc(documentType)
                .orElseThrow(() -> new ResourceNotFoundException("Active legal document not found."));
    }

    private Map<UUID, ConsentRecord> getActiveConsentByDocumentId(User currentUser, List<LegalDocument> activeDocuments) {
        List<UUID> activeDocumentIds = activeDocuments.stream()
                .map(LegalDocument::getId)
                .toList();

        return consentRecordRepository
                .findByUser_IdAndLegalDocument_IdInAndRevokedAtIsNull(currentUser.getId(), activeDocumentIds)
                .stream()
                .collect(Collectors.toMap(
                        consentRecord -> consentRecord.getLegalDocument().getId(),
                        Function.identity()
                ));
    }

    private void revokeOlderActiveConsentsForType(
            User currentUser,
            LegalDocumentType documentType,
            UUID currentLegalDocumentId,
            OffsetDateTime revokedAt
    ) {
        consentRecordRepository.findByUser_IdAndConsentTypeAndRevokedAtIsNull(currentUser.getId(), documentType)
                .stream()
                .filter(consentRecord -> !currentLegalDocumentId.equals(consentRecord.getLegalDocument().getId()))
                .forEach(consentRecord -> consentRecord.setRevokedAt(revokedAt));
    }

    private void updateDirectUserAcceptanceTimestamp(
            User currentUser,
            LegalDocumentType consentType,
            OffsetDateTime acceptedAt
    ) {
        switch (consentType) {
            case TERMS_OF_SERVICE -> currentUser.setTermsAcceptedAt(acceptedAt);
            case PRIVACY_POLICY -> currentUser.setPrivacyAcceptedAt(acceptedAt);
            case MEDICAL_DISCLAIMER -> currentUser.setMedicalDisclaimerAcceptedAt(acceptedAt);
            case COMPANION_CONSENT, COMPANION_DATA_SHARING -> {
                // Optional companion consents stay in consent_records only.
            }
        }
    }

    private boolean isRequiredLegalAcceptance(LegalDocumentType consentType) {
        return switch (consentType) {
            case TERMS_OF_SERVICE, PRIVACY_POLICY, MEDICAL_DISCLAIMER -> true;
            case COMPANION_CONSENT, COMPANION_DATA_SHARING -> false;
        };
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.length() <= maxLength ? trimmedValue : trimmedValue.substring(0, maxLength);
    }

    private static int getDocumentOrder(LegalDocumentType documentType) {
        return switch (documentType) {
            case TERMS_OF_SERVICE -> 0;
            case PRIVACY_POLICY -> 1;
            case MEDICAL_DISCLAIMER -> 2;
            case COMPANION_CONSENT -> 3;
            case COMPANION_DATA_SHARING -> 4;
        };
    }
}
