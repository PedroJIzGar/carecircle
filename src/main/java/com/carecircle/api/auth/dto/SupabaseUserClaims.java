package com.carecircle.api.auth.dto;

/**
 * Normalized subset of Supabase Auth JWT claims used by the application layer.
 *
 * <p>The security module will build this object from the validated JWT in a later
 * step. The user domain receives this DTO instead of depending directly on Spring
 * Security classes.</p>
 *
 * @param supabaseUserId Supabase Auth subject claim.
 * @param email verified or unverified email claim supplied by Supabase.
 * @param fullName display name from user metadata when available.
 * @param avatarUrl avatar URL from user metadata when available.
 * @param emailVerified whether Supabase marks the email as verified.
 */
public record SupabaseUserClaims(
        String supabaseUserId,
        String email,
        String fullName,
        String avatarUrl,
        boolean emailVerified
) {
}
