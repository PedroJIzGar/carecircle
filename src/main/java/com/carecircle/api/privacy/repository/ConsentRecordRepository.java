package com.carecircle.api.privacy.repository;

import com.carecircle.api.privacy.entity.ConsentRecord;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for user consent records.
 */
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    /**
     * Finds active consents for a user and a set of legal document ids.
     *
     * @param userId user identifier.
     * @param legalDocumentIds legal document identifiers.
     * @return active consent records.
     */
    @EntityGraph(attributePaths = {"legalDocument"})
    List<ConsentRecord> findByUser_IdAndLegalDocument_IdInAndRevokedAtIsNull(
            UUID userId,
            Collection<UUID> legalDocumentIds
    );

    /**
     * Finds the active consent for a user and legal document.
     *
     * @param userId user identifier.
     * @param legalDocumentId legal document identifier.
     * @return active consent when present.
     */
    @EntityGraph(attributePaths = {"legalDocument"})
    Optional<ConsentRecord> findByUser_IdAndLegalDocument_IdAndRevokedAtIsNull(UUID userId, UUID legalDocumentId);

    /**
     * Finds active consent records for a consent type.
     *
     * @param userId user identifier.
     * @param consentType consent type.
     * @return active consent records for the type.
     */
    @EntityGraph(attributePaths = {"legalDocument"})
    List<ConsentRecord> findByUser_IdAndConsentTypeAndRevokedAtIsNull(UUID userId, LegalDocumentType consentType);

    /**
     * Finds a consent owned by a user.
     *
     * @param id consent record identifier.
     * @param userId user identifier.
     * @return consent when it belongs to the user.
     */
    @EntityGraph(attributePaths = {"legalDocument"})
    Optional<ConsentRecord> findByIdAndUser_Id(UUID id, UUID userId);
}
