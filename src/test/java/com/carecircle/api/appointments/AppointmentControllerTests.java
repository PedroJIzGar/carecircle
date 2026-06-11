package com.carecircle.api.appointments;

import com.carecircle.api.appointments.entity.Appointment;
import com.carecircle.api.appointments.entity.AppointmentStatus;
import com.carecircle.api.appointments.repository.AppointmentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AppointmentControllerTests {

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
    private AppointmentRepository appointmentRepository;

    @Test
    void createAppointmentCreatesScheduledAppointmentWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("appointment-create-main", "Appointment Create Main");
        User collaborator = createUser("appointment-create-collaborator", "Appointment Create Collaborator");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment create family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Create Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(2).withNano(0);
        OffsetDateTime endsAt = startsAt.plusHours(1);

        mockMvc.perform(post("/circles/{circleId}/appointments", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  GP appointment  ",
                                  "location": "  Health center  ",
                                  "notes": "  Bring ID card  ",
                                  "startsAt": "%s",
                                  "endsAt": "%s"
                                }
                                """.formatted(startsAt, endsAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careCircleId").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.title").value("GP appointment"))
                .andExpect(jsonPath("$.location").value("Health center"))
                .andExpect(jsonPath("$.notes").value("Bring ID card"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.endsAt").isNotEmpty())
                .andExpect(jsonPath("$.createdByUserId").value(mainCaregiver.getId().toString()));

        assertThat(appointmentRepository.findAll())
                .singleElement()
                .satisfies(appointment -> {
                    assertThat(appointment.getTitle()).isEqualTo("GP appointment");
                    assertThat(appointment.getLocation()).isEqualTo("Health center");
                    assertThat(appointment.getNotes()).isEqualTo("Bring ID card");
                    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
                    assertThat(appointment.getCreatedByUser().getId()).isEqualTo(mainCaregiver.getId());
                });
    }

    @Test
    void createAppointmentAllowsCollaboratorAndOptionalFields() throws Exception {
        User mainCaregiver = createUser("appointment-collab-main", "Appointment Collab Main");
        User collaborator = createUser("appointment-collab-user", "Appointment Collab User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment collaborator family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Collaborator Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(3).withNano(0);

        mockMvc.perform(post("/circles/{circleId}/appointments", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Video call",
                                  "startsAt": "%s"
                                }
                                """.formatted(startsAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Video call"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.endsAt").doesNotExist())
                .andExpect(jsonPath("$.createdByUserId").value(collaborator.getId().toString()));
    }

    @Test
    void createAppointmentRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("appointment-observer-main", "Appointment Observer Main");
        User observer = createUser("appointment-observer-user", "Appointment Observer User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment observer family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Observer Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        mockMvc.perform(post("/circles/{circleId}/appointments", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Blocked appointment",
                                  "startsAt": "%s"
                                }
                                """.formatted(OffsetDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can create care circle appointments."));
    }

    @Test
    void createAppointmentReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("appointment-outside-main", "Appointment Outside Main");
        User outsideUser = createUser("appointment-outside-user", "Appointment Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        mockMvc.perform(post("/circles/{circleId}/appointments", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hidden appointment",
                                  "startsAt": "%s"
                                }
                                """.formatted(OffsetDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createAppointmentValidatesTitle() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/appointments", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "appointment-invalid-title-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "startsAt": "%s"
                                }
                                """.formatted(OffsetDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void createAppointmentRejectsInvalidTimeRange() throws Exception {
        User mainCaregiver = createUser("appointment-range-main", "Appointment Range Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment range family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Range Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(1).withNano(0);

        mockMvc.perform(post("/circles/{circleId}/appointments", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid range",
                                  "startsAt": "%s",
                                  "endsAt": "%s"
                                }
                                """.formatted(startsAt, startsAt.minusHours(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("endsAt must be after startsAt."));
    }

    @Test
    void createAppointmentRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/appointments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Unauthenticated appointment",
                                  "startsAt": "%s"
                                }
                                """.formatted(OffsetDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void listAppointmentsReturnsCircleAppointmentsForObserverOrderedByStatusAndStartDate() throws Exception {
        User mainCaregiver = createUser("appointment-list-main", "Appointment List Main");
        User observer = createUser("appointment-list-observer", "Appointment List Observer");
        User otherMainCaregiver = createUser("appointment-list-other-main", "Appointment List Other Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment list family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment List Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Other appointment family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Other Appointment Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherMainCaregiver, CircleRole.MAIN_CAREGIVER));

        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(4).withNano(0);
        appointmentRepository.save(new Appointment(careCircle, "Later scheduled appointment", startsAt.plusHours(2), mainCaregiver));
        appointmentRepository.save(new Appointment(careCircle, "Soon scheduled appointment", startsAt.plusHours(1), mainCaregiver));

        Appointment cancelledAppointment = new Appointment(careCircle, "Cancelled appointment", startsAt.minusHours(1), mainCaregiver);
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
        cancelledAppointment.setCancelledAt(OffsetDateTime.now().withNano(0));
        cancelledAppointment.setCancelledByUser(mainCaregiver);
        appointmentRepository.save(cancelledAppointment);

        appointmentRepository.save(new Appointment(otherCircle, "Other circle appointment", startsAt, otherMainCaregiver));

        mockMvc.perform(get("/circles/{circleId}/appointments", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Soon scheduled appointment"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[1].title").value("Later scheduled appointment"))
                .andExpect(jsonPath("$[1].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[2].title").value("Cancelled appointment"))
                .andExpect(jsonPath("$[2].status").value("CANCELLED"))
                .andExpect(jsonPath("$[?(@.title == 'Other circle appointment')]", hasSize(0)));
    }

    @Test
    void listAppointmentsReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("appointment-list-outside-main", "Appointment List Outside Main");
        User outsideUser = createUser("appointment-list-outside-user", "Appointment List Outside User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment list outside family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment List Outside Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        appointmentRepository.save(new Appointment(
                careCircle,
                "Private appointment",
                OffsetDateTime.now().plusDays(1).withNano(0),
                mainCaregiver
        ));

        mockMvc.perform(get("/circles/{circleId}/appointments", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void updateAppointmentUpdatesEditableFieldsWhenCurrentUserIsCollaborator() throws Exception {
        User mainCaregiver = createUser("appointment-update-main", "Appointment Update Main");
        User collaborator = createUser("appointment-update-collaborator", "Appointment Update Collaborator");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment update family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Update Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        Appointment appointment = appointmentRepository.save(new Appointment(
                careCircle,
                "Old appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        ));
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(5).withNano(0);
        OffsetDateTime endsAt = startsAt.plusHours(2);

        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", careCircle.getId(), appointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Updated appointment  ",
                                  "location": "  Updated location  ",
                                  "notes": "  Updated notes  ",
                                  "startsAt": "%s",
                                  "endsAt": "%s"
                                }
                                """.formatted(startsAt, endsAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointment.getId().toString()))
                .andExpect(jsonPath("$.title").value("Updated appointment"))
                .andExpect(jsonPath("$.location").value("Updated location"))
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.startsAt").isNotEmpty())
                .andExpect(jsonPath("$.endsAt").isNotEmpty());

        assertThat(appointmentRepository.findById(appointment.getId()))
                .isPresent()
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getTitle()).isEqualTo("Updated appointment");
                    assertThat(updated.getLocation()).isEqualTo("Updated location");
                    assertThat(updated.getNotes()).isEqualTo("Updated notes");
                    assertThat(updated.getStartsAt()).isEqualTo(startsAt);
                    assertThat(updated.getEndsAt()).isEqualTo(endsAt);
                });
    }

    @Test
    void updateAppointmentClearsOptionalFields() throws Exception {
        User mainCaregiver = createUser("appointment-clear-main", "Appointment Clear Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment clear family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Clear Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        Appointment appointment = new Appointment(
                careCircle,
                "Clear appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        );
        appointment.setLocation("Old location");
        appointment.setNotes("Old notes");
        appointment.setEndsAt(appointment.getStartsAt().plusHours(1));
        Appointment savedAppointment = appointmentRepository.save(appointment);

        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", careCircle.getId(), savedAppointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clearLocation": true,
                                  "clearNotes": true,
                                  "clearEndsAt": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.endsAt").doesNotExist());

        assertThat(appointmentRepository.findById(savedAppointment.getId()))
                .isPresent()
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getLocation()).isNull();
                    assertThat(updated.getNotes()).isNull();
                    assertThat(updated.getEndsAt()).isNull();
                });
    }

    @Test
    void updateAppointmentRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("appointment-update-observer-main", "Appointment Update Observer Main");
        User observer = createUser("appointment-update-observer-user", "Appointment Update Observer User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment update observer family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Update Observer Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        Appointment appointment = appointmentRepository.save(new Appointment(
                careCircle,
                "Observer update blocked",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        ));

        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", careCircle.getId(), appointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Should fail"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can update care circle appointments."));
    }

    @Test
    void updateAppointmentReturnsNotFoundWhenAppointmentBelongsToAnotherCircle() throws Exception {
        User mainCaregiver = createUser("appointment-update-other-main", "Appointment Update Other Main");
        User otherMainCaregiver = createUser("appointment-update-other-owner", "Appointment Update Other Owner");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment update current family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Update Current Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Appointment update other family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Appointment Update Other Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherMainCaregiver, CircleRole.MAIN_CAREGIVER));
        Appointment otherAppointment = appointmentRepository.save(new Appointment(
                otherCircle,
                "Other appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                otherMainCaregiver
        ));

        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", careCircle.getId(), otherAppointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Should stay hidden"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Appointment not found."));
    }

    @Test
    void updateAppointmentRejectsCancelledAppointment() throws Exception {
        User mainCaregiver = createUser("appointment-update-cancelled-main", "Appointment Update Cancelled Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment update cancelled family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Update Cancelled Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        Appointment appointment = new Appointment(
                careCircle,
                "Cancelled appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        );
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(OffsetDateTime.now().withNano(0));
        appointment.setCancelledByUser(mainCaregiver);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", careCircle.getId(), savedAppointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Should fail"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Only scheduled appointments can be updated."));
    }

    @Test
    void updateAppointmentRejectsConflictingClearAndSet() throws Exception {
        User mainCaregiver = createUser("appointment-update-conflict-main", "Appointment Update Conflict Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment update conflict family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Update Conflict Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        Appointment appointment = appointmentRepository.save(new Appointment(
                careCircle,
                "Conflict appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        ));

        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", careCircle.getId(), appointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location": "New location",
                                  "clearLocation": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("location cannot be set and cleared in the same request."));
    }

    @Test
    void updateAppointmentRejectsInvalidTimeRange() throws Exception {
        User mainCaregiver = createUser("appointment-update-range-main", "Appointment Update Range Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment update range family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Update Range Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        Appointment appointment = appointmentRepository.save(new Appointment(
                careCircle,
                "Range appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        ));

        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(3).withNano(0);

        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", careCircle.getId(), appointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startsAt": "%s",
                                  "endsAt": "%s"
                                }
                                """.formatted(startsAt, startsAt.minusMinutes(30))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("endsAt must be after startsAt."));
    }

    @Test
    void updateAppointmentRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(patch("/circles/{circleId}/appointments/{appointmentId}", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Unauthenticated update"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void cancelAppointmentMarksScheduledAppointmentCancelledWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("appointment-cancel-main", "Appointment Cancel Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment cancel family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Cancel Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        Appointment appointment = appointmentRepository.save(new Appointment(
                careCircle,
                "Cancel appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        ));

        mockMvc.perform(post("/circles/{circleId}/appointments/{appointmentId}/cancel", careCircle.getId(), appointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointment.getId().toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()))
                .andExpect(jsonPath("$.cancelledByUserId").value(mainCaregiver.getId().toString()));

        assertThat(appointmentRepository.findById(appointment.getId()))
                .isPresent()
                .get()
                .satisfies(cancelled -> {
                    assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
                    assertThat(cancelled.getCancelledAt()).isNotNull();
                    assertThat(cancelled.getCancelledByUser().getId()).isEqualTo(mainCaregiver.getId());
                });
    }

    @Test
    void cancelAppointmentRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("appointment-cancel-observer-main", "Appointment Cancel Observer Main");
        User observer = createUser("appointment-cancel-observer-user", "Appointment Cancel Observer User");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment cancel observer family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Cancel Observer Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        Appointment appointment = appointmentRepository.save(new Appointment(
                careCircle,
                "Observer cancel blocked",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        ));

        mockMvc.perform(post("/circles/{circleId}/appointments/{appointmentId}/cancel", careCircle.getId(), appointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can cancel care circle appointments."));
    }

    @Test
    void cancelAppointmentReturnsNotFoundWhenAppointmentBelongsToAnotherCircle() throws Exception {
        User mainCaregiver = createUser("appointment-cancel-other-main", "Appointment Cancel Other Main");
        User otherMainCaregiver = createUser("appointment-cancel-other-owner", "Appointment Cancel Other Owner");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment cancel current family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Cancel Current Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));

        CareCircle otherCircle = careCircleRepository.save(new CareCircle("Appointment cancel other family", otherMainCaregiver));
        elderProfileRepository.save(new ElderProfile(otherCircle, "Appointment Cancel Other Elder"));
        circleMemberRepository.save(new CircleMember(otherCircle, otherMainCaregiver, CircleRole.MAIN_CAREGIVER));
        Appointment otherAppointment = appointmentRepository.save(new Appointment(
                otherCircle,
                "Other cancel appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                otherMainCaregiver
        ));

        mockMvc.perform(post("/circles/{circleId}/appointments/{appointmentId}/cancel", careCircle.getId(), otherAppointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Appointment not found."));
    }

    @Test
    void cancelAppointmentRejectsAlreadyCancelledAppointment() throws Exception {
        User mainCaregiver = createUser("appointment-cancel-already-main", "Appointment Cancel Already Main");

        CareCircle careCircle = careCircleRepository.save(new CareCircle("Appointment cancel already family", mainCaregiver));
        elderProfileRepository.save(new ElderProfile(careCircle, "Appointment Cancel Already Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, mainCaregiver, CircleRole.MAIN_CAREGIVER));
        Appointment appointment = new Appointment(
                careCircle,
                "Already cancelled appointment",
                OffsetDateTime.now().plusDays(2).withNano(0),
                mainCaregiver
        );
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(OffsetDateTime.now().withNano(0));
        appointment.setCancelledByUser(mainCaregiver);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        mockMvc.perform(post("/circles/{circleId}/appointments/{appointmentId}/cancel", careCircle.getId(), savedAppointment.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Only scheduled appointments can be cancelled."));
    }

    @Test
    void cancelAppointmentRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/appointments/{appointmentId}/cancel", UUID.randomUUID(), UUID.randomUUID()))
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
