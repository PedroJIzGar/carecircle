package com.carecircle.api.privacy.repository;

import com.carecircle.api.privacy.entity.LegalDocument;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for versioned legal documents.
 */
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

    /**
     * Returns every document version currently offered to users.
     *
     * @return active legal documents.
     */
    List<LegalDocument> findByActiveTrue();

    /**
     * Returns the current active document for a type.
     *
     * @param documentType requested document type.
     * @return active document when one exists.
     */
    Optional<LegalDocument> findFirstByDocumentTypeAndActiveTrueOrderByPublishedAtDescCreatedAtDesc(
            LegalDocumentType documentType
    );
}
