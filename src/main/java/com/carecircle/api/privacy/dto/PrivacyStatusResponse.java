package com.carecircle.api.privacy.dto;

import java.util.List;

/**
 * Current user's privacy and legal acceptance status.
 *
 * @param documents active document statuses for the authenticated user.
 */
public record PrivacyStatusResponse(
        List<PrivacyDocumentStatusResponse> documents
) {
}
