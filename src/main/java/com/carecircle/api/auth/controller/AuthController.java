package com.carecircle.api.auth.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.users.dto.UserResponse;
import com.carecircle.api.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication-related API endpoints.
 *
 * <p>CareCircle does not authenticate credentials directly. Clients authenticate
 * with Supabase Auth and send the Supabase access token to this backend as a
 * Bearer token.</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authenticated user endpoints")
public class AuthController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    /**
     * Returns the internal CareCircle user for the current Supabase identity.
     *
     * <p>If this is the first authenticated request for the Supabase user, the
     * internal user row is created with safe defaults.</p>
     *
     * @return synchronized internal user.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user",
            description = "Synchronizes the Supabase identity with the internal CareCircle user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public UserResponse me() {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return userService.findOrCreateFromSupabaseClaims(claims);
    }
}
