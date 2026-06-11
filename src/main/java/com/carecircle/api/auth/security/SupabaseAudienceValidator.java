package com.carecircle.api.auth.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Validates the expected Supabase JWT audience.
 *
 * <p>Supabase access tokens commonly use {@code authenticated} as the audience.
 * Spring Security validates issuer and timestamps, but audience checks are
 * application-specific and must be configured explicitly.</p>
 */
public final class SupabaseAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    /**
     * Creates a validator for the configured audience.
     *
     * @param expectedAudience expected value in the JWT {@code aud} claim.
     */
    public SupabaseAudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    /**
     * Verifies that the JWT contains the configured audience.
     *
     * @param jwt validated JWT candidate.
     * @return success when no audience is configured or when the JWT contains it.
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!StringUtils.hasText(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        if (jwt.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The required Supabase audience is missing.",
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
