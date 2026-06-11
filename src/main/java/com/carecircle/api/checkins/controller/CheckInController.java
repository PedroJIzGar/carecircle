package com.carecircle.api.checkins.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.checkins.dto.CheckInResponse;
import com.carecircle.api.checkins.dto.CreateCheckInRequest;
import com.carecircle.api.checkins.service.CheckInService;
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
 * Care circle check-in API endpoints.
 */
@RestController
@RequestMapping("/circles/{circleId}/checkins")
@RequiredArgsConstructor
@Tag(name = "Check-ins", description = "Care circle family check-in endpoints")
public class CheckInController {

    private final CurrentUserProvider currentUserProvider;
    private final CheckInService checkInService;

    /**
     * Creates a non-clinical family check-in in a care circle.
     *
     * @param circleId requested care circle identifier.
     * @param request validated check-in creation request.
     * @return created check-in response.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a care circle check-in",
            description = "Creates a non-clinical family check-in when the authenticated user is MAIN_CAREGIVER or COLLABORATOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CheckInResponse createCheckIn(
            @PathVariable UUID circleId,
            @Valid @RequestBody CreateCheckInRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return checkInService.createCheckIn(claims, circleId, request);
    }

    /**
     * Lists check-ins visible to the authenticated member of a care circle.
     *
     * @param circleId requested care circle identifier.
     * @return ordered check-ins for the requested circle.
     */
    @GetMapping
    @Operation(
            summary = "List care circle check-ins",
            description = "Returns check-ins when the authenticated user is an active member of the care circle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<CheckInResponse> listCheckIns(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return checkInService.listCheckIns(claims, circleId);
    }
}
