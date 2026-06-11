package com.carecircle.api.circles.dto;

import com.carecircle.api.circles.entity.CareCircleStatus;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a care circle aggregate.
 *
 * @param id care circle identifier.
 * @param name circle name.
 * @param description optional circle description.
 * @param status circle lifecycle state.
 * @param createdByUserId internal user that created the circle.
 * @param createdAt creation timestamp.
 * @param updatedAt last update timestamp.
 * @param elderProfile basic elder profile associated with the circle.
 * @param currentMembership membership created for the authenticated user.
 */
public record CareCircleResponse(
        UUID id,
        String name,
        String description,
        CareCircleStatus status,
        UUID createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        ElderProfileResponse elderProfile,
        CircleMemberResponse currentMembership
) {

    /**
     * Basic elder profile response nested inside the care circle response.
     *
     * @param id elder profile identifier.
     * @param fullName elder person's full name.
     * @param preferredName optional preferred name.
     * @param birthDate optional birth date.
     * @param notes optional general family notes.
     */
    public record ElderProfileResponse(
            UUID id,
            String fullName,
            String preferredName,
            LocalDate birthDate,
            String notes
    ) {
    }

    /**
     * Current user's membership response nested inside the care circle response.
     *
     * @param id membership identifier.
     * @param userId internal user identifier.
     * @param role role granted inside the care circle.
     * @param status membership lifecycle state.
     * @param joinedAt timestamp when the user joined the circle.
     */
    public record CircleMemberResponse(
            UUID id,
            UUID userId,
            CircleRole role,
            CircleMemberStatus status,
            OffsetDateTime joinedAt
    ) {
    }
}
