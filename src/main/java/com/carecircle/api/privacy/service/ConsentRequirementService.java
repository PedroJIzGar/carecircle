package com.carecircle.api.privacy.service;

import com.carecircle.api.privacy.entity.ConsentRecord;
import com.carecircle.api.privacy.entity.LegalDocument;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import com.carecircle.api.privacy.repository.ConsentRecordRepository;
import com.carecircle.api.privacy.repository.LegalDocumentRepository;
import com.carecircle.api.shared.exception.ResourceConflictException;
import com.carecircle.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reusable checks for workflows that require accepted legal documents.
 */
@Service
@RequiredArgsConstructor
public class ConsentRequirementService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final ConsentRecordRepository consentRecordRepository;

    /**
     * Requires the user to have active consent records for the current active
     * version of every requested document type.
     *
     * @param user authenticated internal user.
     * @param requiredDocumentTypes legal document types required by the workflow.
     * @param failureMessage API message used when one or more consents are missing.
     * @throws ResourceConflictException when active documents are not configured or consent is missing.
     */
    @Transactional(readOnly = true)
    public void requireActiveConsents(
            User user,
            Collection<LegalDocumentType> requiredDocumentTypes,
            String failureMessage
    ) {
        List<LegalDocument> requiredDocuments = requiredDocumentTypes.stream()
                .map(this::getActiveDocumentOrThrow)
                .toList();
        Set<UUID> acceptedDocumentIds = consentRecordRepository
                .findByUser_IdAndLegalDocument_IdInAndRevokedAtIsNull(
                        user.getId(),
                        requiredDocuments.stream().map(LegalDocument::getId).toList()
                )
                .stream()
                .map(ConsentRecord::getLegalDocument)
                .map(LegalDocument::getId)
                .collect(Collectors.toSet());

        boolean allRequiredDocumentsAccepted = requiredDocuments.stream()
                .map(LegalDocument::getId)
                .allMatch(acceptedDocumentIds::contains);
        if (!allRequiredDocumentsAccepted) {
            throw new ResourceConflictException(failureMessage);
        }
    }

    private LegalDocument getActiveDocumentOrThrow(LegalDocumentType documentType) {
        return legalDocumentRepository
                .findFirstByDocumentTypeAndActiveTrueOrderByPublishedAtDescCreatedAtDesc(documentType)
                .orElseThrow(() -> new ResourceConflictException("Required consent documents are not configured."));
    }
}
