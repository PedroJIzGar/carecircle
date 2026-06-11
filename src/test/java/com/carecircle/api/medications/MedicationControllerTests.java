package com.carecircle.api.medications;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.medications.entity.MedicationIntakeLog;
import com.carecircle.api.medications.entity.MedicationIntakeStatus;
import com.carecircle.api.medications.entity.MedicationReminder;
import com.carecircle.api.medications.entity.MedicationReminderStatus;
import com.carecircle.api.medications.repository.MedicationIntakeLogRepository;
import com.carecircle.api.medications.repository.MedicationReminderRepository;
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

import java.time.LocalDate;
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
class MedicationControllerTests {

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
    private MedicationReminderRepository medicationReminderRepository;

    @Autowired
    private MedicationIntakeLogRepository medicationIntakeLogRepository;

    @Test
    void createReminderCreatesActiveFamilyMedicationReminderWhenCurrentUserIsMainCaregiver() throws Exception {
        User mainCaregiver = createUser("med-create-main", "Medication Create Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication create family");

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(10);

        mockMvc.perform(post("/circles/{circleId}/medication-reminders", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName": "  Family entered med  ",
                                  "dosageText": "  1 tablet  ",
                                  "scheduleText": "  Breakfast  ",
                                  "instructions": "  Use the paper prescription as source  ",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(startDate, endDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careCircleId").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.medicationName").value("Family entered med"))
                .andExpect(jsonPath("$.dosageText").value("1 tablet"))
                .andExpect(jsonPath("$.scheduleText").value("Breakfast"))
                .andExpect(jsonPath("$.instructions").value("Use the paper prescription as source"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdByUserId").value(mainCaregiver.getId().toString()));

        assertThat(medicationReminderRepository.findAll())
                .singleElement()
                .satisfies(reminder -> {
                    assertThat(reminder.getMedicationName()).isEqualTo("Family entered med");
                    assertThat(reminder.getDosageText()).isEqualTo("1 tablet");
                    assertThat(reminder.getScheduleText()).isEqualTo("Breakfast");
                    assertThat(reminder.getInstructions()).isEqualTo("Use the paper prescription as source");
                    assertThat(reminder.getStatus()).isEqualTo(MedicationReminderStatus.ACTIVE);
                });
    }

    @Test
    void createReminderRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("med-observer-main", "Medication Observer Main");
        User observer = createUser("med-observer-user", "Medication Observer User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication observer family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));

        mockMvc.perform(post("/circles/{circleId}/medication-reminders", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName": "Blocked med",
                                  "scheduleText": "Morning"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can manage care circle medication reminders."));
    }

    @Test
    void createReminderReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("med-outside-main", "Medication Outside Main");
        User outsideUser = createUser("med-outside-user", "Medication Outside User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication outside family");

        mockMvc.perform(post("/circles/{circleId}/medication-reminders", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName": "Hidden med",
                                  "scheduleText": "Morning"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createReminderRejectsInvalidDateRange() throws Exception {
        User mainCaregiver = createUser("med-range-main", "Medication Range Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication range family");

        LocalDate startDate = LocalDate.now();

        mockMvc.perform(post("/circles/{circleId}/medication-reminders", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName": "Invalid range med",
                                  "scheduleText": "Morning",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(startDate, startDate.minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("endDate must be on or after startDate."));
    }

    @Test
    void listRemindersReturnsCircleRemindersForObserverOrderedByStatusAndName() throws Exception {
        User mainCaregiver = createUser("med-list-main", "Medication List Main");
        User observer = createUser("med-list-observer", "Medication List Observer");
        User otherMainCaregiver = createUser("med-list-other-main", "Medication List Other Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication list family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        CareCircle otherCircle = createCircleWithMember(otherMainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication other family");

        MedicationReminder betaReminder = medicationReminderRepository.save(new MedicationReminder(
                careCircle,
                "Beta med",
                "Night",
                mainCaregiver
        ));
        MedicationReminder alphaReminder = medicationReminderRepository.save(new MedicationReminder(
                careCircle,
                "Alpha med",
                "Morning",
                mainCaregiver
        ));
        MedicationReminder archivedReminder = new MedicationReminder(careCircle, "Archived med", "Lunch", mainCaregiver);
        archivedReminder.setStatus(MedicationReminderStatus.ARCHIVED);
        archivedReminder.setArchivedAt(OffsetDateTime.now().withNano(0));
        archivedReminder.setArchivedByUser(mainCaregiver);
        medicationReminderRepository.save(archivedReminder);
        medicationReminderRepository.save(new MedicationReminder(otherCircle, "Other med", "Morning", otherMainCaregiver));

        mockMvc.perform(get("/circles/{circleId}/medication-reminders", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(alphaReminder.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].id").value(betaReminder.getId().toString()))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$[2].status").value("ARCHIVED"));
    }

    @Test
    void updateReminderUpdatesAndClearsEditableFields() throws Exception {
        User mainCaregiver = createUser("med-update-main", "Medication Update Main");
        User collaborator = createUser("med-update-collab", "Medication Update Collaborator");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication update family");
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));

        MedicationReminder reminder = new MedicationReminder(careCircle, "Old med", "Old schedule", mainCaregiver);
        reminder.setDosageText("Old dose");
        reminder.setInstructions("Old instructions");
        reminder.setStartDate(LocalDate.now());
        reminder.setEndDate(LocalDate.now().plusDays(7));
        MedicationReminder savedReminder = medicationReminderRepository.save(reminder);

        mockMvc.perform(patch("/circles/{circleId}/medication-reminders/{reminderId}", careCircle.getId(), savedReminder.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName": "  Updated med  ",
                                  "scheduleText": "  Updated schedule  ",
                                  "clearDosageText": true,
                                  "clearInstructions": true,
                                  "clearEndDate": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationName").value("Updated med"))
                .andExpect(jsonPath("$.scheduleText").value("Updated schedule"))
                .andExpect(jsonPath("$.dosageText").doesNotExist())
                .andExpect(jsonPath("$.instructions").doesNotExist())
                .andExpect(jsonPath("$.endDate").doesNotExist());

        assertThat(medicationReminderRepository.findById(savedReminder.getId()))
                .isPresent()
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getMedicationName()).isEqualTo("Updated med");
                    assertThat(updated.getScheduleText()).isEqualTo("Updated schedule");
                    assertThat(updated.getDosageText()).isNull();
                    assertThat(updated.getInstructions()).isNull();
                    assertThat(updated.getEndDate()).isNull();
                });
    }

    @Test
    void updateReminderRejectsArchivedReminder() throws Exception {
        User mainCaregiver = createUser("med-update-archived-main", "Medication Update Archived Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication update archived family");
        MedicationReminder reminder = archivedReminder(careCircle, mainCaregiver, "Archived med");

        mockMvc.perform(patch("/circles/{circleId}/medication-reminders/{reminderId}", careCircle.getId(), reminder.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduleText": "Should fail"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Only active medication reminders can be updated."));
    }

    @Test
    void archiveReminderMarksActiveReminderArchived() throws Exception {
        User mainCaregiver = createUser("med-archive-main", "Medication Archive Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication archive family");
        MedicationReminder reminder = medicationReminderRepository.save(new MedicationReminder(
                careCircle,
                "Archive med",
                "Morning",
                mainCaregiver
        ));

        mockMvc.perform(post("/circles/{circleId}/medication-reminders/{reminderId}/archive", careCircle.getId(), reminder.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedAt", notNullValue()))
                .andExpect(jsonPath("$.archivedByUserId").value(mainCaregiver.getId().toString()));

        assertThat(medicationReminderRepository.findById(reminder.getId()))
                .isPresent()
                .get()
                .satisfies(archived -> {
                    assertThat(archived.getStatus()).isEqualTo(MedicationReminderStatus.ARCHIVED);
                    assertThat(archived.getArchivedAt()).isNotNull();
                    assertThat(archived.getArchivedByUser().getId()).isEqualTo(mainCaregiver.getId());
                });
    }

    @Test
    void archiveReminderRejectsAlreadyArchivedReminder() throws Exception {
        User mainCaregiver = createUser("med-archive-again-main", "Medication Archive Again Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication archive again family");
        MedicationReminder reminder = archivedReminder(careCircle, mainCaregiver, "Already archived med");

        mockMvc.perform(post("/circles/{circleId}/medication-reminders/{reminderId}/archive", careCircle.getId(), reminder.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Only active medication reminders can be archived."));
    }

    @Test
    void createIntakeLogCreatesManualLogForActiveReminder() throws Exception {
        User mainCaregiver = createUser("med-log-main", "Medication Log Main");
        User collaborator = createUser("med-log-collab", "Medication Log Collaborator");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication log family");
        circleMemberRepository.save(new CircleMember(careCircle, collaborator, CircleRole.COLLABORATOR));
        MedicationReminder reminder = medicationReminderRepository.save(new MedicationReminder(
                careCircle,
                "Log med",
                "Dinner",
                mainCaregiver
        ));
        OffsetDateTime occurredAt = OffsetDateTime.now().minusHours(1).withNano(0);

        mockMvc.perform(post("/circles/{circleId}/medication-intake-logs", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(collaborator.getSupabaseUserId())
                                .claim("email", collaborator.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reminderId": "%s",
                                  "status": "TAKEN",
                                  "occurredAt": "%s",
                                  "note": "  Logged after dinner  "
                                }
                                """.formatted(reminder.getId(), occurredAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reminderId").value(reminder.getId().toString()))
                .andExpect(jsonPath("$.medicationName").value("Log med"))
                .andExpect(jsonPath("$.status").value("TAKEN"))
                .andExpect(jsonPath("$.note").value("Logged after dinner"))
                .andExpect(jsonPath("$.recordedByUserId").value(collaborator.getId().toString()));

        assertThat(medicationIntakeLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getReminder().getId()).isEqualTo(reminder.getId());
                    assertThat(log.getStatus()).isEqualTo(MedicationIntakeStatus.TAKEN);
                    assertThat(log.getNote()).isEqualTo("Logged after dinner");
                    assertThat(log.getRecordedByUser().getId()).isEqualTo(collaborator.getId());
                });
    }

    @Test
    void createIntakeLogRejectsObserverRequester() throws Exception {
        User mainCaregiver = createUser("med-log-observer-main", "Medication Log Observer Main");
        User observer = createUser("med-log-observer-user", "Medication Log Observer User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication log observer family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        MedicationReminder reminder = medicationReminderRepository.save(new MedicationReminder(
                careCircle,
                "Observer log med",
                "Morning",
                mainCaregiver
        ));

        mockMvc.perform(post("/circles/{circleId}/medication-intake-logs", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reminderId": "%s",
                                  "status": "TAKEN"
                                }
                                """.formatted(reminder.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message")
                        .value("Only main caregivers and collaborators can record medication intake logs."));
    }

    @Test
    void createIntakeLogRejectsArchivedReminder() throws Exception {
        User mainCaregiver = createUser("med-log-archived-main", "Medication Log Archived Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication log archived family");
        MedicationReminder reminder = archivedReminder(careCircle, mainCaregiver, "Archived log med");

        mockMvc.perform(post("/circles/{circleId}/medication-intake-logs", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reminderId": "%s",
                                  "status": "SKIPPED"
                                }
                                """.formatted(reminder.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Only active medication reminders can receive intake logs."));
    }

    @Test
    void createIntakeLogReturnsNotFoundWhenReminderBelongsToAnotherCircle() throws Exception {
        User mainCaregiver = createUser("med-log-other-main", "Medication Log Other Main");
        User otherMainCaregiver = createUser("med-log-other-owner", "Medication Log Other Owner");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication log current family");
        CareCircle otherCircle = createCircleWithMember(otherMainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication log other family");
        MedicationReminder otherReminder = medicationReminderRepository.save(new MedicationReminder(
                otherCircle,
                "Other reminder",
                "Morning",
                otherMainCaregiver
        ));

        mockMvc.perform(post("/circles/{circleId}/medication-intake-logs", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reminderId": "%s",
                                  "status": "TAKEN"
                                }
                                """.formatted(otherReminder.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Medication reminder not found."));
    }

    @Test
    void listIntakeLogsReturnsCircleLogsForObserverOrderedByOccurredAtDescending() throws Exception {
        User mainCaregiver = createUser("med-log-list-main", "Medication Log List Main");
        User observer = createUser("med-log-list-observer", "Medication Log List Observer");
        User otherMainCaregiver = createUser("med-log-list-other-main", "Medication Log List Other Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication log list family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        CareCircle otherCircle = createCircleWithMember(otherMainCaregiver, CircleRole.MAIN_CAREGIVER, "Medication log list other family");

        MedicationReminder reminder = medicationReminderRepository.save(new MedicationReminder(careCircle, "List med", "Morning", mainCaregiver));
        MedicationReminder otherReminder = medicationReminderRepository.save(new MedicationReminder(otherCircle, "Other med", "Morning", otherMainCaregiver));
        OffsetDateTime occurredAt = OffsetDateTime.now().minusDays(1).withNano(0);

        medicationIntakeLogRepository.save(new MedicationIntakeLog(
                careCircle,
                reminder,
                MedicationIntakeStatus.SKIPPED,
                occurredAt.minusHours(2),
                mainCaregiver
        ));
        MedicationIntakeLog recentLog = new MedicationIntakeLog(
                careCircle,
                reminder,
                MedicationIntakeStatus.TAKEN,
                occurredAt.minusHours(1),
                mainCaregiver
        );
        recentLog.setNote("Recent log");
        medicationIntakeLogRepository.save(recentLog);
        medicationIntakeLogRepository.save(new MedicationIntakeLog(
                otherCircle,
                otherReminder,
                MedicationIntakeStatus.TAKEN,
                occurredAt,
                otherMainCaregiver
        ));

        mockMvc.perform(get("/circles/{circleId}/medication-intake-logs", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("TAKEN"))
                .andExpect(jsonPath("$[0].note").value("Recent log"))
                .andExpect(jsonPath("$[1].status").value("SKIPPED"));
    }

    @Test
    void medicationEndpointsRequireBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles/{circleId}/medication-reminders", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/circles/{circleId}/medication-intake-logs", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private CareCircle createCircleWithMember(User user, CircleRole role, String name) {
        CareCircle careCircle = careCircleRepository.save(new CareCircle(name, user));
        elderProfileRepository.save(new ElderProfile(careCircle, name + " Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, user, role));
        return careCircle;
    }

    private MedicationReminder archivedReminder(CareCircle careCircle, User user, String medicationName) {
        MedicationReminder reminder = new MedicationReminder(careCircle, medicationName, "Morning", user);
        reminder.setStatus(MedicationReminderStatus.ARCHIVED);
        reminder.setArchivedAt(OffsetDateTime.now().withNano(0));
        reminder.setArchivedByUser(user);
        return medicationReminderRepository.save(reminder);
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
