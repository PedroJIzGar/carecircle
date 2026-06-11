package com.carecircle.api.circles;

import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CareCircleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CareCircleRepository careCircleRepository;

    @Autowired
    private ElderProfileRepository elderProfileRepository;

    @Autowired
    private CircleMemberRepository circleMemberRepository;

    @Test
    void createCareCircleCreatesAggregateAndMainCaregiverMembership() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        String email = "circle-" + UUID.randomUUID() + "@example.com";

        String responseBody = mockMvc.perform(post("/circles")
                        .with(jwt().jwt(token -> token
                                .subject(supabaseUserId)
                                .claim("email", email)
                                .claim("full_name", "Circle Creator")
                                .claim("email_verified", true)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circle": {
                                    "name": "Garcia family",
                                    "description": "Daily care coordination"
                                  },
                                  "elderProfile": {
                                    "fullName": "Maria Garcia",
                                    "preferredName": "Maria",
                                    "birthDate": "1945-03-12",
                                    "notes": "Prefers morning calls"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Garcia family"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdByUserId", notNullValue()))
                .andExpect(jsonPath("$.elderProfile.id", notNullValue()))
                .andExpect(jsonPath("$.elderProfile.fullName").value("Maria Garcia"))
                .andExpect(jsonPath("$.currentMembership.id", notNullValue()))
                .andExpect(jsonPath("$.currentMembership.role").value("MAIN_CAREGIVER"))
                .andExpect(jsonPath("$.currentMembership.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID careCircleId = UUID.fromString(JsonPath.read(responseBody, "$.id"));
        User user = userRepository.findBySupabaseUserId(supabaseUserId).orElseThrow();
        ElderProfile elderProfile = elderProfileRepository.findByCareCircle_Id(careCircleId).orElseThrow();
        CircleMember member = circleMemberRepository.findByCareCircle_IdAndUser_Id(careCircleId, user.getId()).orElseThrow();

        assertThat(careCircleRepository.existsById(careCircleId)).isTrue();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(elderProfile.getFullName()).isEqualTo("Maria Garcia");
        assertThat(member.getRole()).isEqualTo(CircleRole.MAIN_CAREGIVER);
    }

    @Test
    void createCareCircleRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(post("/circles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circle": {
                                    "name": "Garcia family"
                                  },
                                  "elderProfile": {
                                    "fullName": "Maria Garcia"
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCareCircleValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/circles")
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "invalid-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "circle": {
                                    "name": " "
                                  },
                                  "elderProfile": {
                                    "fullName": "Maria Garcia"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", notNullValue()));
    }
}
