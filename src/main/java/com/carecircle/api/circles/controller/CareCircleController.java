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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
