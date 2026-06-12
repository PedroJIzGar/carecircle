package com.carecircle.api.circles;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.shared.audit.entity.AuditAction;
import com.carecircle.api.shared.audit.entity.AuditEntityType;
import com.carecircle.api.shared.audit.repository.AuditLogRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void updateCareCircleUpdatesBasicsWhenCurrentUserIsMainCaregiver() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "patch-main-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Old family", currentUser));
        careCircle.setDescription("Old description");
        elderProfileRepository.save(new ElderProfile(careCircle, "Patch Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, currentUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(patch("/circles/{circleId}", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated family",
                                  "description": "Updated description"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.name").value("Updated family"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.currentMembership.role").value("MAIN_CAREGIVER"));

        CareCircle updatedCircle = careCircleRepository.findById(careCircle.getId()).orElseThrow();
        assertThat(updatedCircle.getName()).isEqualTo("Updated family");
        assertThat(updatedCircle.getDescription()).isEqualTo("Updated description");
        assertThat(auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                AuditEntityType.CARE_CIRCLE.name(),
                careCircle.getId()
        ))
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getAction()).isEqualTo(AuditAction.CARE_CIRCLE_UPDATED.name());
                    assertThat(auditLog.getActorUser().getId()).isEqualTo(currentUser.getId());
                    assertThat(auditLog.getMetadata()).containsKey("changedFields");
                });
    }

    @Test
    void updateCareCircleCanClearDescriptionWithBlankValue() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "patch-clear-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Clear family", currentUser));
        careCircle.setDescription("Description to clear");
        elderProfileRepository.save(new ElderProfile(careCircle, "Clear Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, currentUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(patch("/circles/{circleId}", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": " "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist());

        CareCircle updatedCircle = careCircleRepository.findById(careCircle.getId()).orElseThrow();
        assertThat(updatedCircle.getDescription()).isNull();
    }

    @Test
    void updateCareCircleRejectsCollaborator() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "patch-collaborator-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Collaborator family", currentUser));
        elderProfileRepository.save(new ElderProfile(careCircle, "Collaborator Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, currentUser, CircleRole.COLLABORATOR));

        mockMvc.perform(patch("/circles/{circleId}", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked update"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only the main caregiver can update care circle details."));
    }

    @Test
    void updateCareCircleReturnsNotFoundWhenUserIsNotActiveMember() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "patch-denied-current-" + UUID.randomUUID() + "@example.com"
        ));
        User otherUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "patch-denied-other-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Patch private family", otherUser));
        elderProfileRepository.save(new ElderProfile(careCircle, "Patch Private Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, otherUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(patch("/circles/{circleId}", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Hidden update"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateCareCircleValidatesBlankName() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "patch-invalid-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateCareCircleRequiresAtLeastOneField() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "patch-empty-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateCareCircleRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthorized update"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCareCircleReturnsCircleWhenCurrentUserHasActiveMembership() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "get-current-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Get family", currentUser));
        elderProfileRepository.save(new ElderProfile(careCircle, "Get Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, currentUser, CircleRole.OBSERVER));

        mockMvc.perform(get("/circles/{circleId}", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.name").value("Get family"))
                .andExpect(jsonPath("$.elderProfile.fullName").value("Get Elder"))
                .andExpect(jsonPath("$.currentMembership.role").value("OBSERVER"));
    }

    @Test
    void getCareCircleReturnsNotFoundWhenUserIsNotActiveMember() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "get-denied-current-" + UUID.randomUUID() + "@example.com"
        ));
        User otherUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "get-denied-other-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Private family", otherUser));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Private Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(get("/circles/{circleId}", otherCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Care circle not found."));
    }

    @Test
    void getCareCircleReturnsNotFoundWhenMembershipWasRemoved() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "get-removed-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Removed detail family", currentUser));
        elderProfileRepository.save(new ElderProfile(careCircle, "Removed Detail Elder"));
        CircleMember membership = new CircleMember(careCircle, currentUser, CircleRole.COLLABORATOR);
        membership.setStatus(CircleMemberStatus.REMOVED);
        circleMemberRepository.save(membership);

        mockMvc.perform(get("/circles/{circleId}", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                        )))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCareCircleRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles/{circleId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCareCirclesReturnsOnlyActiveMembershipsForCurrentUser() throws Exception {
        User currentUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "current-" + UUID.randomUUID() + "@example.com"
        ));
        User otherUser = userRepository.save(new User(
                UUID.randomUUID().toString(),
                "other-" + UUID.randomUUID() + "@example.com"
        ));

        CareCircle visibleCircle = careCircleRepository.save(new CareCircle("Visible family", currentUser));
        elderProfileRepository.save(new ElderProfile(visibleCircle, "Visible Elder"));
        circleMemberRepository.save(new CircleMember(visibleCircle, currentUser, CircleRole.COLLABORATOR));

        CareCircle removedCircle = careCircleRepository.save(new CareCircle("Removed family", currentUser));
        elderProfileRepository.save(new ElderProfile(removedCircle, "Removed Elder"));
        CircleMember removedMembership = new CircleMember(removedCircle, currentUser, CircleRole.OBSERVER);
        removedMembership.setStatus(CircleMemberStatus.REMOVED);
        circleMemberRepository.save(removedMembership);

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Other family", otherUser));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Other Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherUser, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(get("/circles")
                        .with(jwt().jwt(token -> token
                                .subject(currentUser.getSupabaseUserId())
                                .claim("email", currentUser.getEmail())
                                .claim("email_verified", true)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(visibleCircle.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Visible family"))
                .andExpect(jsonPath("$[0].elderProfile.fullName").value("Visible Elder"))
                .andExpect(jsonPath("$[0].currentMembership.role").value("COLLABORATOR"));
    }

    @Test
    void listCareCirclesReturnsEmptyArrayWhenUserHasNoMemberships() throws Exception {
        mockMvc.perform(get("/circles")
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "empty-" + UUID.randomUUID() + "@example.com")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listCareCirclesRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles"))
                .andExpect(status().isUnauthorized());
    }

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
        assertThat(auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                AuditEntityType.CARE_CIRCLE.name(),
                careCircleId
        ))
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getAction()).isEqualTo(AuditAction.CARE_CIRCLE_CREATED.name());
                    assertThat(auditLog.getActorUser().getId()).isEqualTo(user.getId());
                    assertThat(auditLog.getMetadata())
                            .containsEntry("elderProfileId", elderProfile.getId().toString())
                            .containsEntry("mainCaregiverMembershipId", member.getId().toString())
                            .containsEntry("creatorRole", "MAIN_CAREGIVER");
                });
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
