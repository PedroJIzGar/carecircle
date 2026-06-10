package com.carecircle.api.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External configuration required to validate Supabase Auth JWTs.
 *
 * <p>Values are supplied through environment variables or the local `.env` file.
 * No sensitive value should be hardcoded in `application.yml`.</p>
 *
 * @param projectUrl public Supabase project URL.
 * @param issuer expected JWT issuer.
 * @param jwksUri JSON Web Key Set endpoint used to verify JWT signatures.
 * @param audience expected JWT audience, usually {@code authenticated}.
 */
@ConfigurationProperties(prefix = "carecircle.supabase")
public record SupabaseJwtProperties(
        String projectUrl,
        String issuer,
        String jwksUri,
        String audience
) {
}
