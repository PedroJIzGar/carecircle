package com.carecircle.api.members.dto;

import com.carecircle.api.members.entity.CircleRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for changing a non-owner member role inside a care circle.
 *
 * @param role new role to grant.
 */
public record UpdateCircleMemberRoleRequest(
        @NotNull
        CircleRole role
) {

    /**
     * Restricts this endpoint to non-owner roles.
     *
     * @return true when the requested role can be granted by this endpoint.
     */
    @AssertTrue(message = "role must be COLLABORATOR or OBSERVER.")
    public boolean isRoleAllowedForMemberUpdate() {
        return role == null || role == CircleRole.COLLABORATOR || role == CircleRole.OBSERVER;
    }
}
