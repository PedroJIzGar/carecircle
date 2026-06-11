package com.carecircle.api.auth.security;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Reads the authenticated Supabase user from Spring Security's context.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final SupabaseJwtClaimsExtractor claimsExtractor;

    /**
     * Returns normalized Supabase claims for the current request.
     *
     * @return authenticated Supabase user claims.
     * @throws AuthenticationCredentialsNotFoundException when no validated JWT is available.
     */
    public SupabaseUserClaims getRequiredClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication) || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated Supabase JWT is required.");
        }

        return claimsExtractor.extract(jwtAuthentication.getToken());
    }
}
