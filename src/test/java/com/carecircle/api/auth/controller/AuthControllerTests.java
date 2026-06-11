package com.carecircle.api.auth.controller;

import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void meCreatesInternalUserFromJwtClaims() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        String email = "user-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(get("/auth/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject(supabaseUserId)
                                        .claim("email", email)
                                        .claim("full_name", "Care User")
                                        .claim("avatar_url", "https://example.com/avatar.png")
                                        .claim("email_verified", true)
                                )
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.supabaseUserId").value(supabaseUserId))
                .andExpect(jsonPath("$.fullName").value("Care User"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.globalRole").value("USER"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.lastLoginAt", notNullValue()));

        User user = userRepository.findBySupabaseUserId(supabaseUserId).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getFullName()).isEqualTo("Care User");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void meRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void meRejectsJwtWithoutRequiredEmailClaim() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication is required or token is invalid."))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }
}
