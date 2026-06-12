package com.carecircle.api.members;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CircleMemberControllerTests {

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
    void removeCircleMemberMarksRegularMemberAsRemovedWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("member-remove-main", "Remove Main");
        User collaborator = createUser("member-remove-collaborator", "Remove Collaborator");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Remove member family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Remove Member Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CircleMember collaboratorMembership = circleMemberRepository.save(
                new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR)
        );

        mockMvc.perform(delete("/circles/{circleId}/members/{memberId}", careCircle.getId(), collaboratorMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isNoContent());

        assertThat(circleMemberRepository.findById(collaboratorMembership.getId()))
                .isPresent()
                .get()
                .satisfies(member -> {
                    assertThat(member.getStatus()).isEqualTo(CircleMemberStatus.REMOVED);
                    assertThat(member.getRemovedAt()).isNotNull();
                });
        assertThat(auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                AuditEntityType.CIRCLE_MEMBER.name(),
                collaboratorMembership.getId()
        ))
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getAction()).isEqualTo(AuditAction.CIRCLE_MEMBER_REMOVED.name());
                    assertThat(auditLog.getActorUser().getId()).isEqualTo(mainCaregiver.getId());
                    assertThat(auditLog.getMetadata())
                            .containsEntry("careCircleId", careCircle.getId().toString())
                            .containsEntry("targetUserId", collaborator.getId().toString())
                            .containsEntry("role", "COLLABORATOR");
                });
    }

    @Test
    void removeCircleMemberRejectsCollaboratorRequester() throws Exception {
        User mainCaregiver = createUser("member-remove-denied-main", "Remove Denied Main");
        User collaborator = createUser("member-remove-denied-collaborator", "Remove Denied Collaborator");
        User targetUser = createUser("member-remove-denied-target", "Remove Denied Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Remove denied family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Remove Denied Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        CircleMember targetMembership = circleMemberRepository.save(
                new CircleMember(careCircle, targetUser, CircleRole.OBSERVER)
        );

        mockMvc.perform(delete("/circles/{circleId}/members/{memberId}", careCircle.getId(), targetMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only the main caregiver can remove care circle members."));
    }

    @Test
    void removeCircleMemberReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("member-remove-outside-main", "Remove Outside Main");
        User outsideUser = createUser("member-remove-outside-user", "Remove Outside User");
        User targetUser = createUser("member-remove-outside-target", "Remove Outside Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Remove outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Remove Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CircleMember targetMembership = circleMemberRepository.save(
                new CircleMember(careCircle, targetUser, CircleRole.OBSERVER)
        );

        mockMvc.perform(delete("/circles/{circleId}/members/{memberId}", careCircle.getId(), targetMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void removeCircleMemberReturnsNotFoundWhenTargetMemberBelongsToAnotherCircle() throws Exception {
        User mainCaregiver = createUser("member-remove-other-main", "Remove Other Main");
        User otherMainCaregiver = createUser("member-remove-other-owner", "Remove Other Owner");
        User targetUser = createUser("member-remove-other-target", "Remove Other Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Remove current family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Remove Current Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Remove other family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Remove Other Elder"));
        CircleMember otherMembership = circleMemberRepository.save(
                new CircleMember(otherCircle, targetUser, CircleRole.OBSERVER)
        );

        mockMvc.perform(delete("/circles/{circleId}/members/{memberId}", careCircle.getId(), otherMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Care circle member not found."));
    }

    @Test
    void removeCircleMemberRejectsMainCaregiverTarget() throws Exception {
        User mainCaregiver = createUser("member-remove-owner-main", "Remove Owner Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Remove owner family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Remove Owner Elder"));
        CircleMember mainMembership = circleMemberRepository.save(
                new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER)
        );

        mockMvc.perform(delete("/circles/{circleId}/members/{memberId}", careCircle.getId(), mainMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Main caregiver removal requires a dedicated flow."));
    }

    @Test
    void removeCircleMemberRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(delete("/circles/{circleId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCircleMemberRoleUpdatesCollaboratorToObserverWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("member-role-main", "Role Main");
        User collaborator = createUser("member-role-collaborator", "Role Collaborator");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Role update family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Role Update Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CircleMember collaboratorMembership = circleMemberRepository.save(
                new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR)
        );

        mockMvc.perform(patch("/circles/{circleId}/members/{memberId}", careCircle.getId(), collaboratorMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "OBSERVER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(collaboratorMembership.getId().toString()))
                .andExpect(jsonPath("$.userId").value(collaborator.getId().toString()))
                .andExpect(jsonPath("$.role").value("OBSERVER"));

        assertThat(circleMemberRepository.findById(collaboratorMembership.getId()))
                .isPresent()
                .get()
                .extracting(CircleMember::getRole)
                .isEqualTo(CircleRole.OBSERVER);
        assertThat(auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                AuditEntityType.CIRCLE_MEMBER.name(),
                collaboratorMembership.getId()
        ))
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getAction()).isEqualTo(AuditAction.CIRCLE_MEMBER_ROLE_UPDATED.name());
                    assertThat(auditLog.getActorUser().getId()).isEqualTo(mainCaregiver.getId());
                    assertThat(auditLog.getMetadata())
                            .containsEntry("careCircleId", careCircle.getId().toString())
                            .containsEntry("targetUserId", collaborator.getId().toString())
                            .containsEntry("previousRole", "COLLABORATOR")
                            .containsEntry("newRole", "OBSERVER");
                });
    }

    @Test
    void updateCircleMemberRoleRejectsCollaboratorRequester() throws Exception {
        User mainCaregiver = createUser("member-role-denied-main", "Role Denied Main");
        User collaborator = createUser("member-role-denied-collaborator", "Role Denied Collaborator");
        User targetUser = createUser("member-role-denied-target", "Role Denied Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Role denied family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Role Denied Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        CircleMember targetMembership = circleMemberRepository.save(
                new CircleMember(careCircle, targetUser, CircleRole.OBSERVER)
        );

        mockMvc.perform(patch("/circles/{circleId}/members/{memberId}", careCircle.getId(), targetMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "COLLABORATOR"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only the main caregiver can update care circle member roles."));
    }

    @Test
    void updateCircleMemberRoleReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("member-role-outside-main", "Role Outside Main");
        User outsideUser = createUser("member-role-outside-user", "Role Outside User");
        User targetUser = createUser("member-role-outside-target", "Role Outside Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Role outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Role Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        CircleMember targetMembership = circleMemberRepository.save(
                new CircleMember(careCircle, targetUser, CircleRole.OBSERVER)
        );

        mockMvc.perform(patch("/circles/{circleId}/members/{memberId}", careCircle.getId(), targetMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "COLLABORATOR"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateCircleMemberRoleReturnsNotFoundWhenTargetMemberBelongsToAnotherCircle() throws Exception {
        User mainCaregiver = createUser("member-role-other-main", "Role Other Main");
        User otherMainCaregiver = createUser("member-role-other-owner", "Role Other Owner");
        User targetUser = createUser("member-role-other-target", "Role Other Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Role current family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Role Current Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Role other family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Role Other Elder"));
        CircleMember otherMembership = circleMemberRepository.save(
                new CircleMember(otherCircle, targetUser, CircleRole.OBSERVER)
        );

        mockMvc.perform(patch("/circles/{circleId}/members/{memberId}", careCircle.getId(), otherMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "COLLABORATOR"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Care circle member not found."));
    }

    @Test
    void updateCircleMemberRoleRejectsMainCaregiverTarget() throws Exception {
        User mainCaregiver = createUser("member-role-owner-main", "Role Owner Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Role owner family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Role Owner Elder"));
        CircleMember mainMembership = circleMemberRepository.save(
                new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER)
        );

        mockMvc.perform(patch("/circles/{circleId}/members/{memberId}", careCircle.getId(), mainMembership.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "OBSERVER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Main caregiver role changes require a dedicated flow."));
    }

    @Test
    void updateCircleMemberRoleRejectsMainCaregiverRole() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "member-role-invalid-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "MAIN_CAREGIVER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateCircleMemberRoleRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "OBSERVER"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCircleMemberAddsExistingUserWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("member-add-main", "Add Main");
        User targetUser = createUser("member-add-target", "Add Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Add member family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Add Member Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(post("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "COLLABORATOR"
                                }
                                """.formatted(targetUser.getEmail().toUpperCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUser.getId().toString()))
                .andExpect(jsonPath("$.email").value(targetUser.getEmail()))
                .andExpect(jsonPath("$.role").value("COLLABORATOR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(circleMemberRepository.findByCareCircle_IdAndUser_Id(careCircle.getId(), targetUser.getId()))
                .isPresent()
                .get()
                .extracting(CircleMember::getRole)
                .isEqualTo(CircleRole.COLLABORATOR);
        CircleMember savedMembership = circleMemberRepository
                .findByCareCircle_IdAndUser_Id(careCircle.getId(), targetUser.getId())
                .orElseThrow();
        assertThat(auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
                AuditEntityType.CIRCLE_MEMBER.name(),
                savedMembership.getId()
        ))
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getAction()).isEqualTo(AuditAction.CIRCLE_MEMBER_ADDED.name());
                    assertThat(auditLog.getActorUser().getId()).isEqualTo(mainCaregiver.getId());
                    assertThat(auditLog.getMetadata())
                            .containsEntry("careCircleId", careCircle.getId().toString())
                            .containsEntry("targetUserId", targetUser.getId().toString())
                            .containsEntry("role", "COLLABORATOR");
                });
    }

    @Test
    void addCircleMemberRejectsCollaboratorRequester() throws Exception {
        User mainCaregiver = createUser("member-add-denied-main", "Denied Main");
        User collaborator = createUser("member-add-denied-collaborator", "Denied Collaborator");
        User targetUser = createUser("member-add-denied-target", "Denied Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Denied add family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Denied Add Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        mockMvc.perform(post("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "OBSERVER"
                                }
                                """.formatted(targetUser.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only the main caregiver can add care circle members."));
    }

    @Test
    void addCircleMemberReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("member-add-outside-main", "Outside Main");
        User outsideUser = createUser("member-add-outside-user", "Outside User");
        User targetUser = createUser("member-add-outside-target", "Outside Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Outside add family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Outside Add Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(post("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "OBSERVER"
                                }
                                """.formatted(targetUser.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void addCircleMemberReturnsNotFoundWhenTargetUserDoesNotExist() throws Exception {
        User mainCaregiver = createUser("member-add-missing-main", "Missing Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Missing target family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Missing Target Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(post("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing-user@example.com",
                                  "role": "OBSERVER"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found."));
    }

    @Test
    void addCircleMemberReturnsConflictWhenUserAlreadyAssociated() throws Exception {
        User mainCaregiver = createUser("member-add-conflict-main", "Conflict Main");
        User targetUser = createUser("member-add-conflict-target", "Conflict Target");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Conflict add family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Conflict Add Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, targetUser, CircleRole.OBSERVER));

        mockMvc.perform(post("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "COLLABORATOR"
                                }
                                """.formatted(targetUser.getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User is already associated with this care circle."));
    }

    @Test
    void addCircleMemberRejectsMainCaregiverRole() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/members", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "member-invalid-role-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "target@example.com",
                                  "role": "MAIN_CAREGIVER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void addCircleMemberValidatesEmail() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/members", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "member-invalid-email-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "role": "OBSERVER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void addCircleMemberRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/members", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "target@example.com",
                                  "role": "OBSERVER"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCircleMembersReturnsActiveMembersWhenCurrentUserBelongsToCircle() throws Exception {
        User mainCaregiver = createUser("member-main", "Main Caregiver");
        User collaborator = createUser("member-collaborator", "Collaborator User");
        User removedUser = createUser("member-removed", "Removed User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Member family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Member Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        CircleMember removedMembership = new CircleMember(careCircle, removedUser, CircleRole.OBSERVER);
        removedMembership.setStatus(CircleMemberStatus.REMOVED);
        circleMemberRepository.save(removedMembership);

        mockMvc.perform(get("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.email == '" + mainCaregiver.getEmail() + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.email == '" + collaborator.getEmail() + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.email == '" + removedUser.getEmail() + "')]", hasSize(0)))
                .andExpect(jsonPath("$[?(@.email == '" + mainCaregiver.getEmail() + "')].role", hasItem("MAIN_CAREGIVER")))
                .andExpect(jsonPath("$[?(@.email == '" + collaborator.getEmail() + "')].role", hasItem("COLLABORATOR")))
                .andExpect(jsonPath("$[?(@.email == '" + mainCaregiver.getEmail() + "')].supabaseUserId").doesNotExist())
                .andExpect(jsonPath("$[?(@.email == '" + mainCaregiver.getEmail() + "')].phone").doesNotExist());
    }

    @Test
    void listCircleMembersAllowsCollaboratorToReadMembers() throws Exception {
        User mainCaregiver = createUser("member-read-main", "Main Reader");
        User collaborator = createUser("member-read-collaborator", "Collaborator Reader");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Readable member family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Readable Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        mockMvc.perform(get("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listCircleMembersReturnsNotFoundWhenCurrentUserDoesNotBelongToCircle() throws Exception {
        User mainCaregiver = createUser("member-private-main", "Private Main");
        User outsideUser = createUser("member-private-outside", "Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Private member family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Private Member Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(get("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listCircleMembersReturnsNotFoundWhenCurrentMembershipWasRemoved() throws Exception {
        User mainCaregiver = createUser("member-removed-main", "Removed Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Removed member family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Removed Member Elder"));
        CircleMember membership = new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER);
        membership.setStatus(CircleMemberStatus.REMOVED);
        circleMemberRepository.save(membership);

        mockMvc.perform(get("/circles/{circleId}/members", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listCircleMembersRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles/{circleId}/members", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String prefix, String fullName) {
        User user = new User(
                UUID.randomUUID().toString(),
                prefix + "-" + UUID.randomUUID() + "@example.com"
        );
        user.setFullName(fullName);
        user.setAvatarUrl("https://example.com/" + prefix + ".png");
        return userRepository.save(user);
    }
}
