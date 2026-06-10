package com.carecircle.api.auth.security;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseJwtClaimsExtractorTests {

    private final SupabaseJwtClaimsExtractor extractor = new SupabaseJwtClaimsExtractor();

    @Test
    void extractsTopLevelClaims() {
        Jwt jwt = jwtBuilder()
                .claim("email", "user@example.com")
                .claim("full_name", "Care User")
                .claim("avatar_url", "https://example.com/avatar.png")
                .claim("email_verified", true)
                .build();

        SupabaseUserClaims claims = extractor.extract(jwt);

        assertThat(claims.supabaseUserId()).isEqualTo("supabase-user-id");
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.fullName()).isEqualTo("Care User");
        assertThat(claims.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(claims.emailVerified()).isTrue();
    }

    @Test
    void extractsNestedMetadataClaimsWhenTopLevelValuesAreMissing() {
        Jwt jwt = jwtBuilder()
                .claim("email", "user@example.com")
                .claim("user_metadata", Map.of(
                        "full_name", "Nested User",
                        "avatar_url", "https://example.com/nested.png",
                        "email_verified", "true"
                ))
                .build();

        SupabaseUserClaims claims = extractor.extract(jwt);

        assertThat(claims.fullName()).isEqualTo("Nested User");
        assertThat(claims.avatarUrl()).isEqualTo("https://example.com/nested.png");
        assertThat(claims.emailVerified()).isTrue();
    }

    private Jwt.Builder jwtBuilder() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("supabase-user-id")
                .issuer("https://example.supabase.co/auth/v1")
                .audience(List.of("authenticated"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600));
    }
}
