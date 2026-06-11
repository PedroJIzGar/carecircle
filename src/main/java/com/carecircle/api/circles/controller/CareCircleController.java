package com.carecircle.api.circles.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.circles.dto.CareCircleResponse;
import com.carecircle.api.circles.dto.CreateCareCircleRequest;
import com.carecircle.api.circles.service.CareCircleService;
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
 * Care circle API endpoints.
 */
@RestController
@RequestMapping("/circles")
@RequiredArgsConstructor
@Tag(name = "Care Circles", description = "Care circle coordination endpoints")
public class CareCircleController {

    private final CurrentUserProvider currentUserProvider;
    private final CareCircleService careCircleService;

    /**
     * Creates a care circle for the current authenticated user.
     *
     * @param request validated care circle creation request.
     * @return created care circle aggregate.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a care circle",
            description = "Creates a care circle, its basic elder profile and assigns the authenticated user as MAIN_CAREGIVER.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CareCircleResponse createCareCircle(@Valid @RequestBody CreateCareCircleRequest request) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return careCircleService.createCareCircle(claims, request);
    }

    /**
     * Lists care circles visible to the current authenticated user.
     *
     * @return active care circles where the user has an active membership.
     */
    @GetMapping
    @Operation(
            summary = "List current user's care circles",
            description = "Returns care circles where the authenticated user has an active membership.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<CareCircleResponse> listCareCircles() {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return careCircleService.listCurrentUserCareCircles(claims);
    }

    /**
     * Returns one care circle visible to the current authenticated user.
     *
     * @param circleId requested care circle identifier.
     * @return requested care circle aggregate.
     */
    @GetMapping("/{circleId}")
    @Operation(
            summary = "Get a care circle",
            description = "Returns one care circle only when the authenticated user has an active membership.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CareCircleResponse getCareCircle(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return careCircleService.getCurrentUserCareCircle(claims, circleId);
    }
}
