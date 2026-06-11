package com.carecircle.api.elderprofiles.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.circles.dto.CareCircleResponse;
import com.carecircle.api.elderprofiles.dto.UpdateElderProfileRequest;
import com.carecircle.api.elderprofiles.service.ElderProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Elder profile API endpoints scoped to a care circle.
 */
@RestController
@RequestMapping("/circles/{circleId}/elder-profile")
@RequiredArgsConstructor
@Tag(name = "Elder Profiles", description = "Basic non-clinical elder profile endpoints")
public class ElderProfileController {

    private final CurrentUserProvider currentUserProvider;
    private final ElderProfileService elderProfileService;

    /**
     * Updates the elder profile for a care circle managed by the current user.
     *
     * @param circleId requested care circle identifier.
     * @param request validated partial update request.
     * @return updated care circle aggregate.
     */
    @PatchMapping
    @Operation(
            summary = "Update an elder profile",
            description = "Updates basic non-clinical elder profile fields when the authenticated user is the MAIN_CAREGIVER.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CareCircleResponse updateElderProfile(
            @PathVariable UUID circleId,
            @Valid @RequestBody UpdateElderProfileRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return elderProfileService.updateElderProfile(claims, circleId, request);
    }
}
