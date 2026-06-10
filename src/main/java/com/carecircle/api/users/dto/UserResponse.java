package com.carecircle.api.users.dto;

import com.carecircle.api.users.entity.AccountStatus;
import com.carecircle.api.users.entity.GlobalRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public API representation of the internal CareCircle user.
 *
 * <p>The DTO intentionally excludes sensitive authentication material. Passwords
 * are not part of CareCircle because Supabase Auth is the identity provider.</p>
 *
 * @param id internal CareCircle user identifier.
 * @param supabaseUserId Supabase Auth subject claim.
 * @param fullName optional display name.
 * @param email user email address.
 * @param globalRole application-level role.
 * @param accountStatus internal account status.
 * @param emailVerified email verification state reported by Supabase.
 * @param createdAt user creation timestamp.
 * @param updatedAt last user update timestamp.
 * @param lastLoginAt last backend synchronization/login timestamp.
 */
public record UserResponse(
        UUID id,
        String supabaseUserId,
        String fullName,
        String email,
        GlobalRole globalRole,
        AccountStatus accountStatus,
        boolean emailVerified,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastLoginAt
) {
}
