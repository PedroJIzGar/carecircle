package com.carecircle.api.members.dto;

import com.carecircle.api.members.entity.CircleRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for adding an existing CareCircle user to a care circle.
 *
 * <p>This endpoint does not create identity accounts. The target user must
 * already exist internally, which means they have authenticated through Supabase
 * and have been synchronized by CareCircle.</p>
 *
 * @param email existing user's email address.
 * @param role role to grant inside the circle.
 */
public record AddCircleMemberRequest(
        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotNull
        CircleRole role
) {

    /**
     * Restricts this endpoint to non-owner roles.
     *
     * @return true when the requested role can be granted by this endpoint.
     */
    @AssertTrue(message = "role must be COLLABORATOR or OBSERVER.")
    public boolean isRoleAllowedForMemberAdd() {
        return role == null || role == CircleRole.COLLABORATOR || role == CircleRole.OBSERVER;
    }
}
