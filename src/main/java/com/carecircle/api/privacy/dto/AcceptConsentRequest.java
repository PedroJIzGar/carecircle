package com.carecircle.api.privacy.dto;

import com.carecircle.api.privacy.entity.LegalDocumentType;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for accepting the active version of a legal document.
 *
 * @param documentType legal document type to accept.
 */
public record AcceptConsentRequest(
        @NotNull
        LegalDocumentType documentType
) {
}
