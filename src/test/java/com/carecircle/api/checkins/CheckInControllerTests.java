package com.carecircle.api.checkins;

import com.carecircle.api.checkins.entity.CheckIn;
import com.carecircle.api.checkins.entity.CheckInStatus;
import com.carecircle.api.checkins.repository.CheckInRepository;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckInControllerTests {

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
    private CheckInRepository checkInRepository;

    @Test
    void createCheckInCreatesFamilyCheckInWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("checkin-create-main", "CheckIn Create Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Check-in create family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Check-in Create Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        OffsetDateTime checkedAt = OffsetDateTime.now().minusHours(2).withNano(0);

        mockMvc.perform(post("/circles/{circleId}/checkins", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OK",
                                  "note": "  Morning call went well  ",
                                  "checkedAt": "%s"
                                }
                                """.formatted(checkedAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careCircleId").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.note").value("Morning call went well"))
                .andExpect(jsonPath("$.checkedAt").isNotEmpty())
                .andExpect(jsonPath("$.createdByUserId").value(mainCaregiver.getId().toString()))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        assertThat(checkInRepository.findAll())
                .singleElement()
                .satisfies(checkIn -> {
                    assertThat(checkIn.getStatus()).isEqualTo(CheckInStatus.OK);
                    assertThat(checkIn.getNote()).isEqualTo("Morning call went well");
                    assertThat(checkIn.getCheckedAt()).isEqualTo(checkedAt);
                    assertThat(checkIn.getCreatedByUser().getId()).isEqualTo(mainCaregiver.getId());
                });
    }

    @Test
    void createCheckInDefaultsCheckedAtAndAllowsCollaborator() throws Exception {
        User mainCaregiver = createUser("checkin-collab-main", "CheckIn Collab Main");
        User collaborator = createUser("checkin-collab-user", "CheckIn Collab User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Check-in collaborator family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Check-in Collaborator Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        mockMvc.perform(post("/circles/{circleId}/checkins", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "NEEDS_ATTENTION"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEEDS_ATTENTION"))
                .andExpect(jsonPath("$.checkedAt", notNullValue()))
                .andExpect(jsonPath("$.createdByUserId").value(collaborator.getId().toString()));

        assertThat(checkInRepository.findAll())
                .singleElement()
                .satisfies(checkIn -> {
                    assertThat(checkIn.getStatus()).isEqualTo(CheckInStatus.NEEDS_ATTENTION);
                    assertThat(checkIn.getCheckedAt()).isNotNull();
                    assertThat(checkIn.getCreatedByUser().getId()).isEqualTo(collaborator.getId());
                });
    }

    @Test
    void createCheckInRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("checkin-observer-main", "CheckIn Observer Main");
        User observer = createUser("checkin-observer-user", "CheckIn Observer User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Check-in observer family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Check-in Observer Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        mockMvc.perform(post("/circles/{circleId}/checkins", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OK"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can create care circle check-ins."));
    }

    @Test
    void createCheckInReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("checkin-outside-main", "CheckIn Outside Main");
        User outsideUser = createUser("checkin-outside-user", "CheckIn Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Check-in outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Check-in Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(post("/circles/{circleId}/checkins", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OK"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createCheckInValidatesMissingStatus() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/checkins", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "checkin-invalid-status-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Missing status"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    void createCheckInRejectsFutureCheckedAt() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/checkins", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "checkin-future-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OK",
                                  "checkedAt": "%s"
                                }
                                """.formatted(OffsetDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("checkedAt"));
    }

    @Test
    void createCheckInRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/checkins", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OK"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void listCheckInsReturnsCircleCheckInsForObserverOrderedByCheckedAtDescending() throws Exception {
        User mainCaregiver = createUser("checkin-list-main", "CheckIn List Main");
        User collaborator = createUser("checkin-list-collaborator", "CheckIn List Collaborator");
        User observer = createUser("checkin-list-observer", "CheckIn List Observer");
        User otherMainCaregiver = createUser("checkin-list-other-main", "CheckIn List Other Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Check-in list family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Check-in List Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Other check-in family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Other Check-in Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherMainCaregiver, CircleRole.MAIN_CAREGIVER));

        OffsetDateTime checkedAt = OffsetDateTime.now().minusDays(1).withNano(0);
        checkInRepository.save(new CheckIn(careCircle, CheckInStatus.OK, checkedAt.minusHours(2), mainCaregiver));
        CheckIn recentCheckIn = new CheckIn(careCircle, CheckInStatus.NEEDS_ATTENTION, checkedAt.minusHours(1), collaborator);
        recentCheckIn.setNote("Recent update");
        checkInRepository.save(recentCheckIn);
        checkInRepository.save(new CheckIn(careCircle, CheckInStatus.NO_RESPONSE, checkedAt.minusHours(3), mainCaregiver));
        checkInRepository.save(new CheckIn(otherCircle, CheckInStatus.OK, checkedAt, otherMainCaregiver));

        mockMvc.perform(get("/circles/{circleId}/checkins", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].status").value("NEEDS_ATTENTION"))
                .andExpect(jsonPath("$[0].note").value("Recent update"))
                .andExpect(jsonPath("$[0].createdByUserId").value(collaborator.getId().toString()))
                .andExpect(jsonPath("$[1].status").value("OK"))
                .andExpect(jsonPath("$[2].status").value("NO_RESPONSE"));
    }

    @Test
    void listCheckInsReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("checkin-list-outside-main", "CheckIn List Outside Main");
        User outsideUser = createUser("checkin-list-outside-user", "CheckIn List Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Check-in list outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Check-in List Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        checkInRepository.save(new CheckIn(
                careCircle,
                CheckInStatus.OK,
                OffsetDateTime.now().minusHours(1).withNano(0),
                mainCaregiver
        ));

        mockMvc.perform(get("/circles/{circleId}/checkins", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listCheckInsRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles/{circleId}/checkins", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private User createUser(String prefix, String fullName) {
        User user = new User(
                UUID.randomUUID().toString(),
                prefix + "-" + UUID.randomUUID() + "@example.com"
        );
        user.setFullName(fullName);
        return userRepository.save(user);
    }
}
