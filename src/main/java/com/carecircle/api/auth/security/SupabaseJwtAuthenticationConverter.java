package com.carecircle.api.auth.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * Converts a validated Supabase JWT into a Spring Security authentication token.
 *
 * <p>CareCircle does not derive business permissions from JWT claims. Circle roles
 * and global roles are resolved from the database, so this converter only marks
 * the request as authenticated and uses the JWT subject as the principal name.</p>
 */
public final class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Converts the JWT into an authenticated token with no application authorities.
     *
     * @param jwt validated Supabase JWT.
     * @return Spring Security authentication token.
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject());
    }
}
