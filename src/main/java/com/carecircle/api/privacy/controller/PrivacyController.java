package com.carecircle.api.privacy.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.privacy.dto.AcceptConsentRequest;
import com.carecircle.api.privacy.dto.ConsentRecordResponse;
import com.carecircle.api.privacy.dto.LegalDocumentResponse;
import com.carecircle.api.privacy.dto.PrivacyStatusResponse;
import com.carecircle.api.privacy.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Privacy, legal document and consent API endpoints.
 */
@RestController
@RequestMapping("/privacy")
@RequiredArgsConstructor
@Tag(name = "Privacy", description = "Legal document and consent endpoints")
public class PrivacyController {

    private final CurrentUserProvider currentUserProvider;
    private final PrivacyService privacyService;

    /**
     * Lists active legal documents.
     *
     * @return active legal documents.
     */
    @GetMapping("/legal-documents")
    @Operation(
            summary = "List active legal documents",
            description = "Returns the currently active terms, privacy policy, disclaimer and companion consent documents.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<LegalDocumentResponse> listLegalDocuments() {
        return privacyService.listActiveLegalDocuments();
    }

    /**
     * Returns the authenticated user's privacy status.
     *
     * @return current user's privacy status.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get my privacy status",
            description = "Returns whether the authenticated user accepted each active legal document version.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public PrivacyStatusResponse getMyPrivacyStatus() {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return privacyService.getCurrentPrivacyStatus(claims);
    }

    /**
     * Accepts an active legal document.
     *
     * @param request acceptance request.
     * @param httpServletRequest current HTTP request.
     * @return active consent record.
     */
    @PostMapping("/consents")
    @Operation(
            summary = "Accept a legal document",
            description = "Creates or returns the active consent record for the requested active document type.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ConsentRecordResponse acceptConsent(
            @Valid @RequestBody AcceptConsentRequest request,
            HttpServletRequest httpServletRequest
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return privacyService.acceptConsent(
                claims,
                request,
                resolveIpAddress(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")
        );
    }

    /**
     * Revokes an optional consent.
     *
     * @param consentRecordId consent record identifier.
     * @return revoked consent record.
     */
    @PostMapping("/consents/{consentRecordId}/revoke")
    @Operation(
            summary = "Revoke an optional consent",
            description = "Revokes optional companion-related consents. Required account legal acceptances cannot be revoked here.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ConsentRecordResponse revokeConsent(@PathVariable UUID consentRecordId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return privacyService.revokeConsent(claims, consentRecordId);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
