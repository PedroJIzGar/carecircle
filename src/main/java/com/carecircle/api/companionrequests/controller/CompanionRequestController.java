package com.carecircle.api.companionrequests.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.companionrequests.dto.CompanionRequestResponse;
import com.carecircle.api.companionrequests.dto.CreateCompanionRequestRequest;
import com.carecircle.api.companionrequests.service.CompanionRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Care circle companion request API endpoints.
 */
@RestController
@RequestMapping("/circles/{circleId}/companion-requests")
@RequiredArgsConstructor
@Tag(name = "Companion Requests", description = "Family companion request endpoints")
public class CompanionRequestController {

    private final CurrentUserProvider currentUserProvider;
    private final CompanionRequestService companionRequestService;

    /**
     * Creates a family companion request.
     *
     * @param circleId requested care circle identifier.
     * @param request validated companion request creation request.
     * @return created companion request response.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a companion request",
            description = "Creates a family companion request. CareCircle records the request but does not assign volunteers directly.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CompanionRequestResponse createCompanionRequest(
            @PathVariable UUID circleId,
            @Valid @RequestBody CreateCompanionRequestRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return companionRequestService.createCompanionRequest(claims, circleId, request);
    }

    /**
     * Lists companion requests visible to the authenticated care circle member.
     *
     * @param circleId requested care circle identifier.
     * @return ordered companion requests for the requested circle.
     */
    @GetMapping
    @Operation(
            summary = "List companion requests",
            description = "Returns companion requests when the authenticated user is an active member of the care circle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<CompanionRequestResponse> listCompanionRequests(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return companionRequestService.listCompanionRequests(claims, circleId);
    }

    /**
     * Cancels an active companion request.
     *
     * @param circleId requested care circle identifier.
     * @param requestId requested companion request identifier.
     * @return cancelled companion request response.
     */
    @PostMapping("/{requestId}/cancel")
    @Operation(
            summary = "Cancel a companion request",
            description = "Cancels a REQUESTED companion request without deleting its history.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CompanionRequestResponse cancelCompanionRequest(
            @PathVariable UUID circleId,
            @PathVariable UUID requestId
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return companionRequestService.cancelCompanionRequest(claims, circleId, requestId);
    }
}
