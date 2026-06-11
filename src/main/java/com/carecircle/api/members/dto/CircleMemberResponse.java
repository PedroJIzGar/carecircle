package com.carecircle.api.members.dto;

import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public API representation of a care circle member.
 *
 * <p>The DTO exposes only the user details needed for family coordination and
 * avoids identity-provider identifiers such as Supabase user id.</p>
 *
 * @param id membership identifier.
 * @param userId internal CareCircle user identifier.
 * @param fullName optional user display name.
 * @param email user email address.
 * @param avatarUrl optional user avatar URL.
 * @param role family role inside the care circle.
 * @param status membership lifecycle state.
 * @param joinedAt timestamp when the user joined the circle.
 * @param createdAt membership creation timestamp.
 */
public record CircleMemberResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String avatarUrl,
        CircleRole role,
        CircleMemberStatus status,
        OffsetDateTime joinedAt,
        OffsetDateTime createdAt
) {
}
