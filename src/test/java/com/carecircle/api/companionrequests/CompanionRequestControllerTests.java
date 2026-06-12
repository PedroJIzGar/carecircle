package com.carecircle.api.companionrequests;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.companionrequests.entity.CompanionRequest;
import com.carecircle.api.companionrequests.entity.CompanionRequestStatus;
import com.carecircle.api.companionrequests.repository.CompanionRequestRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.privacy.entity.ConsentRecord;
import com.carecircle.api.privacy.entity.LegalDocument;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import com.carecircle.api.privacy.repository.ConsentRecordRepository;
import com.carecircle.api.privacy.repository.LegalDocumentRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
class CompanionRequestControllerTests {

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
    private CompanionRequestRepository companionRequestRepository;

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    @Autowired
    private ConsentRecordRepository consentRecordRepository;

    @Test
    void createCompanionRequestCreatesRequestedRequestWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("companion-create-main", "Companion Create Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion create family");
        acceptCompanionConsents(mainCaregiver);
        LocalDate requestedForDate = LocalDate.now().plusDays(3);

        mockMvc.perform(post("/circles/{circleId}/companion-requests", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedForDate": "%s",
                                  "timeWindow": "  Morning  ",
                                  "location": "  Home visit  ",
                                  "reason": "  Conversation and walk  ",
                                  "notes": "  Ring the front door  "
                                }
                                """.formatted(requestedForDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careCircleId").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.requestedByUserId").value(mainCaregiver.getId().toString()))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.requestedForDate").value(requestedForDate.toString()))
                .andExpect(jsonPath("$.timeWindow").value("Morning"))
                .andExpect(jsonPath("$.location").value("Home visit"))
                .andExpect(jsonPath("$.reason").value("Conversation and walk"))
                .andExpect(jsonPath("$.notes").value("Ring the front door"));

        assertThat(companionRequestRepository.findAll())
                .singleElement()
                .satisfies(request -> {
                    assertThat(request.getStatus()).isEqualTo(CompanionRequestStatus.REQUESTED);
                    assertThat(request.getRequestedForDate()).isEqualTo(requestedForDate);
                    assertThat(request.getTimeWindow()).isEqualTo("Morning");
                    assertThat(request.getLocation()).isEqualTo("Home visit");
                    assertThat(request.getRequestedByUser().getId()).isEqualTo(mainCaregiver.getId());
                });
    }

    @Test
    void createCompanionRequestAllowsCollaborator() throws Exception {
        User mainCaregiver = createUser("companion-collab-main", "Companion Collab Main");
        User collaborator = createUser("companion-collab-user", "Companion Collab User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion collaborator family");
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        acceptCompanionConsents(collaborator);

        mockMvc.perform(post("/circles/{circleId}/companion-requests", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedForDate": "%s",
                                  "timeWindow": "Afternoon",
                                  "location": "Community center"
                                }
                                """.formatted(LocalDate.now().plusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestedByUserId").value(collaborator.getId().toString()))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void createCompanionRequestRequiresAcceptedCompanionConsents() throws Exception {
        User mainCaregiver = createUser("companion-consent-main", "Companion Consent Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion consent family");

        mockMvc.perform(post("/circles/{circleId}/companion-requests", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedForDate": "%s",
                                  "timeWindow": "Morning",
                                  "location": "Home"
                                }
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value(
                        "Companion request requires accepted companion consent and data sharing consent."
                ));

        assertThat(companionRequestRepository.findAll()).isEmpty();
    }

    @Test
    void createCompanionRequestRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("companion-observer-main", "Companion Observer Main");
        User observer = createUser("companion-observer-user", "Companion Observer User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion observer family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        mockMvc.perform(post("/circles/{circleId}/companion-requests", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedForDate": "%s",
                                  "timeWindow": "Morning",
                                  "location": "Home"
                                }
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Only main caregivers and collaborators can manage companion requests."));
    }

    @Test
    void createCompanionRequestReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("companion-outside-main", "Companion Outside Main");
        User outsideUser = createUser("companion-outside-user", "Companion Outside User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion outside family");

        mockMvc.perform(post("/circles/{circleId}/companion-requests", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedForDate": "%s",
                                  "timeWindow": "Morning",
                                  "location": "Home"
                                }
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createCompanionRequestValidatesRequiredFields() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/companion-requests", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "companion-invalid-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedForDate": "%s",
                                  "timeWindow": " ",
                                  "location": " "
                                }
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
    }

    @Test
    void createCompanionRequestRejectsPastDate() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/companion-requests", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "companion-past-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedForDate": "%s",
                                  "timeWindow": "Morning",
                                  "location": "Home"
                                }
                                """.formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("requestedForDate"));
    }

    @Test
    void listCompanionRequestsReturnsCircleRequestsForObserverOrderedByStatusAndDate() throws Exception {
        User mainCaregiver = createUser("companion-list-main", "Companion List Main");
        User observer = createUser("companion-list-observer", "Companion List Observer");
        User otherMainCaregiver = createUser("companion-list-other-main", "Companion List Other Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion list family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        CareCircle otherCircle = createCircleWithMember(otherMainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion other family");

        CompanionRequest laterRequest = companionRequestRepository.save(new CompanionRequest(
                careCircle,
                mainCaregiver,
                LocalDate.now().plusDays(5),
                "Afternoon",
                "Home"
        ));
        CompanionRequest soonRequest = companionRequestRepository.save(new CompanionRequest(
                careCircle,
                mainCaregiver,
                LocalDate.now().plusDays(2),
                "Morning",
                "Home"
        ));
        CompanionRequest cancelledRequest = new CompanionRequest(
                careCircle,
                mainCaregiver,
                LocalDate.now().plusDays(1),
                "Evening",
                "Home"
        );
        cancelledRequest.setStatus(CompanionRequestStatus.CANCELLED);
        cancelledRequest.setCancelledAt(OffsetDateTime.now().withNano(0));
        cancelledRequest.setCancelledByUser(mainCaregiver);
        companionRequestRepository.save(cancelledRequest);
        companionRequestRepository.save(new CompanionRequest(
                otherCircle,
                otherMainCaregiver,
                LocalDate.now().plusDays(1),
                "Morning",
                "Other home"
        ));

        mockMvc.perform(get("/circles/{circleId}/companion-requests", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(soonRequest.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$[1].id").value(laterRequest.getId().toString()))
                .andExpect(jsonPath("$[1].status").value("REQUESTED"))
                .andExpect(jsonPath("$[2].status").value("CANCELLED"));
    }

    @Test
    void listCompanionRequestsReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("companion-list-outside-main", "Companion List Outside Main");
        User outsideUser = createUser("companion-list-outside-user", "Companion List Outside User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion list outside family");
        companionRequestRepository.save(new CompanionRequest(
                careCircle,
                mainCaregiver,
                LocalDate.now().plusDays(2),
                "Morning",
                "Home"
        ));

        mockMvc.perform(get("/circles/{circleId}/companion-requests", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void cancelCompanionRequestMarksRequestedRequestCancelled() throws Exception {
        User mainCaregiver = createUser("companion-cancel-main", "Companion Cancel Main");
        User collaborator = createUser("companion-cancel-collab", "Companion Cancel Collaborator");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion cancel family");
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        CompanionRequest request = companionRequestRepository.save(new CompanionRequest(
                careCircle,
                mainCaregiver,
                LocalDate.now().plusDays(2),
                "Morning",
                "Home"
        ));

        mockMvc.perform(post("/circles/{circleId}/companion-requests/{requestId}/cancel", careCircle.getId(), request.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()))
                .andExpect(jsonPath("$.cancelledByUserId").value(collaborator.getId().toString()));

        assertThat(companionRequestRepository.findById(request.getId()))
                .isPresent()
                .get()
                .satisfies(cancelled -> {
                    assertThat(cancelled.getStatus()).isEqualTo(CompanionRequestStatus.CANCELLED);
                    assertThat(cancelled.getCancelledAt()).isNotNull();
                    assertThat(cancelled.getCancelledByUser().getId()).isEqualTo(collaborator.getId());
                });
    }

    @Test
    void cancelCompanionRequestRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("companion-cancel-observer-main", "Companion Cancel Observer Main");
        User observer = createUser("companion-cancel-observer-user", "Companion Cancel Observer User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion cancel observer family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        CompanionRequest request = companionRequestRepository.save(new CompanionRequest(
                careCircle,
                mainCaregiver,
                LocalDate.now().plusDays(2),
                "Morning",
                "Home"
        ));

        mockMvc.perform(post("/circles/{circleId}/companion-requests/{requestId}/cancel", careCircle.getId(), request.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void cancelCompanionRequestReturnsNotFoundWhenRequestBelongsToAnotherCircle() throws Exception {
        User mainCaregiver = createUser("companion-cancel-other-main", "Companion Cancel Other Main");
        User otherMainCaregiver = createUser("companion-cancel-other-owner", "Companion Cancel Other Owner");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion cancel current family");
        CareCircle otherCircle = createCircleWithMember(otherMainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion cancel other family");
        CompanionRequest otherRequest = companionRequestRepository.save(new CompanionRequest(
                otherCircle,
                otherMainCaregiver,
                LocalDate.now().plusDays(2),
                "Morning",
                "Other home"
        ));

        mockMvc.perform(post("/circles/{circleId}/companion-requests/{requestId}/cancel", careCircle.getId(), otherRequest.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Companion request not found."));
    }

    @Test
    void cancelCompanionRequestRejectsAlreadyCancelledRequest() throws Exception {
        User mainCaregiver = createUser("companion-cancel-again-main", "Companion Cancel Again Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Companion cancel again family");
        CompanionRequest request = new CompanionRequest(
                careCircle,
                mainCaregiver,
                LocalDate.now().plusDays(2),
                "Morning",
                "Home"
        );
        request.setStatus(CompanionRequestStatus.CANCELLED);
        request.setCancelledAt(OffsetDateTime.now().withNano(0));
        request.setCancelledByUser(mainCaregiver);
        CompanionRequest savedRequest = companionRequestRepository.save(request);

        mockMvc.perform(post("/circles/{circleId}/companion-requests/{requestId}/cancel", careCircle.getId(), savedRequest.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Only requested companion requests can be cancelled."));
    }

    @Test
    void companionRequestEndpointsRequireBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles/{circleId}/companion-requests", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/circles/{circleId}/companion-requests/{requestId}/cancel", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private CareCircle createCircleWithMember(User user, CircleRole role, String name) {
        CareCircle careCircle = careCircleRepository.save(new CareCircle(name, user));
        elderProfileRepository.save(new ElderProfile(careCircle, name + " Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, user, role));
        return careCircle;
    }

    private void acceptCompanionConsents(User user) {
        acceptConsent(user, LegalDocumentType.COMPANION_CONSENT);
        acceptConsent(user, LegalDocumentType.COMPANION_DATA_SHARING);
    }

    private void acceptConsent(User user, LegalDocumentType documentType) {
        LegalDocument legalDocument = legalDocumentRepository
                .findFirstByDocumentTypeAndActiveTrueOrderByPublishedAtDescCreatedAtDesc(documentType)
                .orElseThrow();
        consentRecordRepository.save(new ConsentRecord(user, legalDocument));
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
