package com.carecircle.api.auth.security;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Extracts CareCircle-relevant identity data from a validated Supabase JWT.
 */
@Component
public class SupabaseJwtClaimsExtractor {

    /**
     * Converts a validated JWT into normalized claims for the user domain.
     *
     * @param jwt validated Supabase JWT.
     * @return normalized user claims consumed by {@code UserService}.
     */
    public SupabaseUserClaims extract(Jwt jwt) {
        String fullName = firstText(
                jwt.getClaimAsString("full_name"),
                jwt.getClaimAsString("name"),
                nestedClaimAsString(jwt, "user_metadata", "full_name"),
                nestedClaimAsString(jwt, "user_metadata", "name"),
                nestedClaimAsString(jwt, "raw_user_meta_data", "full_name"),
                nestedClaimAsString(jwt, "raw_user_meta_data", "name")
        );

        String avatarUrl = firstText(
                jwt.getClaimAsString("avatar_url"),
                jwt.getClaimAsString("picture"),
                nestedClaimAsString(jwt, "user_metadata", "avatar_url"),
                nestedClaimAsString(jwt, "user_metadata", "picture"),
                nestedClaimAsString(jwt, "raw_user_meta_data", "avatar_url"),
                nestedClaimAsString(jwt, "raw_user_meta_data", "picture")
        );

        return new SupabaseUserClaims(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                fullName,
                avatarUrl,
                resolveEmailVerified(jwt)
        );
    }

    private boolean resolveEmailVerified(Jwt jwt) {
        Object value = firstNonNull(
                jwt.getClaims().get("email_verified"),
                nestedClaim(jwt, "user_metadata", "email_verified"),
                nestedClaim(jwt, "raw_user_meta_data", "email_verified")
        );

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return value instanceof String stringValue && Boolean.parseBoolean(stringValue);
    }

    private String nestedClaimAsString(Jwt jwt, String claimName, String nestedName) {
        Object value = nestedClaim(jwt, claimName, nestedName);
        return value instanceof String stringValue ? stringValue : null;
    }

    private Object nestedClaim(Jwt jwt, String claimName, String nestedName) {
        Object claim = jwt.getClaims().get(claimName);
        if (!(claim instanceof Map<?, ?> values)) {
            return null;
        }
        return values.get(nestedName);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
