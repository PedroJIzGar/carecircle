package com.carecircle.api.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseAudienceValidatorTests {

    @Test
    void acceptsExpectedAudience() {
        SupabaseAudienceValidator validator = new SupabaseAudienceValidator("authenticated");

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience("authenticated"));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsMissingExpectedAudience() {
        SupabaseAudienceValidator validator = new SupabaseAudienceValidator("authenticated");

        OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience("anon"));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwtWithAudience(String audience) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("supabase-user-id")
                .issuer("https://example.supabase.co/auth/v1")
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("email", "user@example.com")
                .build();
    }
}
