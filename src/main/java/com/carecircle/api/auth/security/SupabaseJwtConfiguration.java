package com.carecircle.api.auth.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configures JWT validation for Supabase Auth.
 */
@Configuration
@EnableConfigurationProperties(SupabaseJwtProperties.class)
public class SupabaseJwtConfiguration {

    /**
     * Builds a JWT decoder backed by Supabase's JWKS endpoint.
     *
     * <p>The decoder validates signature, issuer, expiration and audience. JWKS
     * allows local signature verification when the Supabase project uses asymmetric
     * signing keys.</p>
     *
     * @param properties Supabase JWT configuration.
     * @return JWT decoder used by Spring Security Resource Server.
     */
    @Bean
    JwtDecoder jwtDecoder(SupabaseJwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audienceValidator = new SupabaseAudienceValidator(properties.audience());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

        return decoder;
    }

    /**
     * Provides the authentication converter used by the resource server filter.
     *
     * @return converter for validated Supabase JWTs.
     */
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> supabaseJwtAuthenticationConverter() {
        return new SupabaseJwtAuthenticationConverter();
    }
}
