package com.carecircle.api.users.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.users.dto.UserResponse;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.mapper.UserMapper;
import com.carecircle.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

/**
 * Application service for internal CareCircle user synchronization.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Finds the internal user linked to a Supabase account or creates it on first access.
     *
     * <p>This method is the bridge between external identity and CareCircle business
     * data. It never authenticates passwords; it assumes the Supabase JWT has already
     * been validated by the security layer.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @return synchronized internal user response.
     */
    @Transactional
    public UserResponse findOrCreateFromSupabaseClaims(SupabaseUserClaims claims) {
        validateClaims(claims);

        User user = userRepository.findBySupabaseUserId(claims.supabaseUserId())
                .orElseGet(() -> new User(claims.supabaseUserId(), claims.email()));

        synchronizeProfile(user, claims);
        user.setLastLoginAt(OffsetDateTime.now());

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    private void validateClaims(SupabaseUserClaims claims) {
        if (claims == null) {
            throw new IllegalArgumentException("Supabase claims are required.");
        }
        if (!StringUtils.hasText(claims.supabaseUserId())) {
            throw new IllegalArgumentException("Supabase user id is required.");
        }
        if (!StringUtils.hasText(claims.email())) {
            throw new IllegalArgumentException("Supabase email is required.");
        }
    }

    private void synchronizeProfile(User user, SupabaseUserClaims claims) {
        user.setEmail(claims.email());
        user.setFullName(blankToNull(claims.fullName()));
        user.setAvatarUrl(blankToNull(claims.avatarUrl()));
        user.setEmailVerified(claims.emailVerified());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
