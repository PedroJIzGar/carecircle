package com.carecircle.api.members.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.members.dto.AddCircleMemberRequest;
import com.carecircle.api.members.dto.CircleMemberResponse;
import com.carecircle.api.members.dto.UpdateCircleMemberRoleRequest;
import com.carecircle.api.members.service.CircleMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Care circle member API endpoints.
 */
@RestController
@RequestMapping("/circles/{circleId}/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Care circle membership endpoints")
public class CircleMemberController {

    private final CurrentUserProvider currentUserProvider;
    private final CircleMemberService circleMemberService;

    /**
     * Lists active members in a care circle visible to the current user.
     *
     * @param circleId requested care circle identifier.
     * @return active care circle members.
     */
    @GetMapping
    @Operation(
            summary = "List care circle members",
            description = "Returns active members when the authenticated user is an active member of the circle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<CircleMemberResponse> listCircleMembers(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return circleMemberService.listCircleMembers(claims, circleId);
    }

    /**
     * Adds an existing CareCircle user to the requested care circle.
     *
     * @param circleId requested care circle identifier.
     * @param request validated add member request.
     * @return created member response.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add a care circle member",
            description = "Adds an existing CareCircle user as COLLABORATOR or OBSERVER when the authenticated user is the MAIN_CAREGIVER.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CircleMemberResponse addCircleMember(
            @PathVariable UUID circleId,
            @Valid @RequestBody AddCircleMemberRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return circleMemberService.addCircleMember(claims, circleId, request);
    }

    /**
     * Updates a non-owner member role inside the requested care circle.
     *
     * @param circleId requested care circle identifier.
     * @param memberId requested membership identifier.
     * @param request validated role update request.
     * @return updated member response.
     */
    @PatchMapping("/{memberId}")
    @Operation(
            summary = "Update a care circle member role",
            description = "Updates a member role between COLLABORATOR and OBSERVER when the authenticated user is the MAIN_CAREGIVER.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public CircleMemberResponse updateCircleMemberRole(
            @PathVariable UUID circleId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateCircleMemberRoleRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return circleMemberService.updateCircleMemberRole(claims, circleId, memberId, request);
    }

    /**
     * Removes a non-owner member from the requested care circle.
     *
     * @param circleId requested care circle identifier.
     * @param memberId requested membership identifier.
     */
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove a care circle member",
            description = "Marks a regular member as REMOVED when the authenticated user is the MAIN_CAREGIVER.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public void removeCircleMember(
            @PathVariable UUID circleId,
            @PathVariable UUID memberId
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        circleMemberService.removeCircleMember(claims, circleId, memberId);
    }
}
