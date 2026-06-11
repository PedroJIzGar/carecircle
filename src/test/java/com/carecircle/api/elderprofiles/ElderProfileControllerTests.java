package com.carecircle.api.elderprofiles;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ElderProfileControllerTests {

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
    void updateElderProfileUpdatesBasicsWhenCurrentUserIsMainCaregiver() throws Exception {
        User currentUser = createUser("elder-main");
        CareCircle careCircle = careCircleRepository.save(new CareCircle("Elder family", currentUser));
        ElderProfile elderProfile = elderProfileRepository.save(new ElderProfile(careCircle, "Old Name"));
        circleMemberRepository.save(new CircleMember(careCircle, currentUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(patch("/circles/{circleId}/elder-profile", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Maria Garcia",
                                  "preferredName": "Maria",
                                  "birthDate": "1945-03-12",
                                  "notes": "Prefers morning calls"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.elderProfile.id").value(elderProfile.getId().toString()))
                .andExpect(jsonPath("$.elderProfile.fullName").value("Maria Garcia"))
                .andExpect(jsonPath("$.elderProfile.preferredName").value("Maria"))
                .andExpect(jsonPath("$.elderProfile.birthDate").value("1945-03-12"))
                .andExpect(jsonPath("$.elderProfile.notes").value("Prefers morning calls"))
                .andExpect(jsonPath("$.currentMembership.role").value("MAIN_CAREGIVER"));

        ElderProfile updatedProfile = elderProfileRepository.findByCareCircle_Id(careCircle.getId()).orElseThrow();
        assertThat(updatedProfile.getFullName()).isEqualTo("Maria Garcia");
        assertThat(updatedProfile.getPreferredName()).isEqualTo("Maria");
        assertThat(updatedProfile.getNotes()).isEqualTo("Prefers morning calls");
    }

    @Test
    void updateElderProfileCanClearOptionalTextFieldsWithBlankValues() throws Exception {
        User currentUser = createUser("elder-clear");
        CareCircle careCircle = careCircleRepository.save(new CareCircle("Clear elder family", currentUser));
        ElderProfile elderProfile = elderProfileRepository.save(new ElderProfile(careCircle, "Maria Garcia"));
        elderProfile.setPreferredName("Maria");
        elderProfile.setNotes("Notes to clear");
        circleMemberRepository.save(new CircleMember(careCircle, currentUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(patch("/circles/{circleId}/elder-profile", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "preferredName": " ",
                                  "notes": " "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elderProfile.preferredName").doesNotExist())
                .andExpect(jsonPath("$.elderProfile.notes").doesNotExist());

        ElderProfile updatedProfile = elderProfileRepository.findByCareCircle_Id(careCircle.getId()).orElseThrow();
        assertThat(updatedProfile.getPreferredName()).isNull();
        assertThat(updatedProfile.getNotes()).isNull();
    }

    @Test
    void updateElderProfileRejectsCollaborator() throws Exception {
        User currentUser = createUser("elder-collaborator");
        CareCircle careCircle = careCircleRepository.save(new CareCircle("Collaborator elder family", currentUser));
        elderProfileRepository.save(new ElderProfile(careCircle, "Maria Garcia"));
        circleMemberRepository.save(new CircleMember(careCircle, currentUser, CircleRole.COLLABORATOR));

        mockMvc.perform(patch("/circles/{circleId}/elder-profile", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Blocked Name"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only the main caregiver can update the elder profile."));
    }

    @Test
    void updateElderProfileReturnsNotFoundWhenUserIsNotActiveMember() throws Exception {
        User currentUser = createUser("elder-denied-current");
        User otherUser = createUser("elder-denied-other");
        CareCircle careCircle = careCircleRepository.save(new CareCircle("Private elder family", otherUser));
        elderProfileRepository.save(new ElderProfile(careCircle, "Private Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, otherUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(patch("/circles/{circleId}/elder-profile", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Hidden Name"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateElderProfileValidatesBlankFullName() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/elder-profile", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "elder-invalid-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateElderProfileValidatesFutureBirthDate() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/elder-profile", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "elder-future-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birthDate": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateElderProfileRequiresAtLeastOneField() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/elder-profile", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "elder-empty-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateElderProfileRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/elder-profile", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Unauthorized Name"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String prefix) {
        return userRepository.save(new User(
                UUID.randomUUID().toString(),
                prefix + "-" + UUID.randomUUID() + "@example.com"
        ));
    }
}
