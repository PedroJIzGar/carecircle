package com.carecircle.api.summaries;

import com.carecircle.api.appointments.entity.Appointment;
import com.carecircle.api.appointments.entity.AppointmentStatus;
import com.carecircle.api.appointments.repository.AppointmentRepository;
import com.carecircle.api.checkins.entity.CheckIn;
import com.carecircle.api.checkins.entity.CheckInStatus;
import com.carecircle.api.checkins.repository.CheckInRepository;
import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.medications.entity.MedicationIntakeLog;
import com.carecircle.api.medications.entity.MedicationIntakeStatus;
import com.carecircle.api.medications.entity.MedicationReminder;
import com.carecircle.api.medications.repository.MedicationIntakeLogRepository;
import com.carecircle.api.medications.repository.MedicationReminderRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.tasks.entity.CareTask;
import com.carecircle.api.tasks.entity.TaskStatus;
import com.carecircle.api.tasks.repository.CareTaskRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SummaryControllerTests {

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
    private CareTaskRepository careTaskRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private MedicationReminderRepository medicationReminderRepository;

    @Autowired
    private MedicationIntakeLogRepository medicationIntakeLogRepository;

    @Test
    void weeklySummaryAggregatesVisibleCircleDataForObserver() throws Exception {
        User mainCaregiver = createUser("summary-main", "Summary Main");
        User observer = createUser("summary-observer", "Summary Observer");
        User otherMainCaregiver = createUser("summary-other-main", "Summary Other Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Summary family");
        circleMemberRepository.save(new CircleMember(careCircle, observer, CircleRole.OBSERVER));
        CareCircle otherCircle = createCircleWithMember(otherMainCaregiver, CircleRole.MAIN_CAREGIVER, "Other summary family");

        LocalDate weekStart = LocalDate.now()
                .minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        OffsetDateTime monday = atHour(weekStart, 9);
        OffsetDateTime tuesday = atHour(weekStart.plusDays(1), 10);
        OffsetDateTime wednesday = atHour(weekStart.plusDays(2), 11);
        OffsetDateTime nextWeek = atHour(weekStart.plusWeeks(1), 9);

        createOpenTask(careCircle, mainCaregiver, "Due this week", monday);
        createCompletedTask(careCircle, mainCaregiver, "Completed this week", tuesday);
        createCompletedTask(careCircle, mainCaregiver, "Completed outside week", nextWeek);
        createOpenTask(otherCircle, otherMainCaregiver, "Other circle due", monday);

        appointmentRepository.save(new Appointment(careCircle, "Scheduled appointment", monday, mainCaregiver));
        Appointment cancelledAppointment = new Appointment(careCircle, "Cancelled appointment", nextWeek, mainCaregiver);
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);
        cancelledAppointment.setCancelledAt(wednesday);
        cancelledAppointment.setCancelledByUser(mainCaregiver);
        appointmentRepository.save(cancelledAppointment);
        appointmentRepository.save(new Appointment(otherCircle, "Other appointment", monday, otherMainCaregiver));

        checkInRepository.save(new CheckIn(careCircle, CheckInStatus.OK, monday, mainCaregiver));
        checkInRepository.save(new CheckIn(careCircle, CheckInStatus.NEEDS_ATTENTION, tuesday, mainCaregiver));
        checkInRepository.save(new CheckIn(careCircle, CheckInStatus.NO_RESPONSE, wednesday, mainCaregiver));
        checkInRepository.save(new CheckIn(careCircle, CheckInStatus.OK, nextWeek, mainCaregiver));
        checkInRepository.save(new CheckIn(otherCircle, CheckInStatus.OK, monday, otherMainCaregiver));

        MedicationReminder reminder = medicationReminderRepository.save(new MedicationReminder(
                careCircle,
                "Summary medication",
                "Morning",
                mainCaregiver
        ));
        MedicationReminder otherReminder = medicationReminderRepository.save(new MedicationReminder(
                otherCircle,
                "Other medication",
                "Morning",
                otherMainCaregiver
        ));
        medicationIntakeLogRepository.save(new MedicationIntakeLog(
                careCircle,
                reminder,
                MedicationIntakeStatus.TAKEN,
                monday,
                mainCaregiver
        ));
        medicationIntakeLogRepository.save(new MedicationIntakeLog(
                careCircle,
                reminder,
                MedicationIntakeStatus.SKIPPED,
                tuesday,
                mainCaregiver
        ));
        medicationIntakeLogRepository.save(new MedicationIntakeLog(
                otherCircle,
                otherReminder,
                MedicationIntakeStatus.TAKEN,
                monday,
                otherMainCaregiver
        ));

        mockMvc.perform(get("/circles/{circleId}/summaries/weekly", careCircle.getId())
                        .param("weekStart", weekStart.toString())
                        .with(jwt().jwt(token -> token
                                .subject(observer.getSupabaseUserId())
                                .claim("email", observer.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careCircleId").value(careCircle.getId().toString()))
                .andExpect(jsonPath("$.weekStart").value(weekStart.toString()))
                .andExpect(jsonPath("$.weekEnd").value(weekStart.plusDays(6).toString()))
                .andExpect(jsonPath("$.tasks.openTasksDueThisWeek").value(1))
                .andExpect(jsonPath("$.tasks.completedTasksThisWeek").value(1))
                .andExpect(jsonPath("$.tasks.overdueOpenTasks").value(1))
                .andExpect(jsonPath("$.appointments.scheduledAppointmentsThisWeek").value(1))
                .andExpect(jsonPath("$.appointments.cancelledAppointmentsThisWeek").value(1))
                .andExpect(jsonPath("$.checkIns.totalCheckIns").value(3))
                .andExpect(jsonPath("$.checkIns.okCheckIns").value(1))
                .andExpect(jsonPath("$.checkIns.needsAttentionCheckIns").value(1))
                .andExpect(jsonPath("$.checkIns.noResponseCheckIns").value(1))
                .andExpect(jsonPath("$.medications.intakeLogsThisWeek").value(2))
                .andExpect(jsonPath("$.medications.takenLogs").value(1))
                .andExpect(jsonPath("$.medications.skippedLogs").value(1));
    }

    @Test
    void weeklySummaryDefaultsToCurrentWeekWhenWeekStartIsOmitted() throws Exception {
        User mainCaregiver = createUser("summary-default-main", "Summary Default Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Summary default family");
        LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        mockMvc.perform(get("/circles/{circleId}/summaries/weekly", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(currentWeekStart.toString()));
    }

    @Test
    void weeklySummaryRejectsNonMondayWeekStart() throws Exception {
        User mainCaregiver = createUser("summary-invalid-main", "Summary Invalid Main");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Summary invalid family");
        LocalDate nonMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(1);

        mockMvc.perform(get("/circles/{circleId}/summaries/weekly", careCircle.getId())
                        .param("weekStart", nonMonday.toString())
                        .with(jwt().jwt(token -> token
                                .subject(mainCaregiver.getSupabaseUserId())
                                .claim("email", mainCaregiver.getEmail())
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("weekStart must be a Monday."));
    }

    @Test
    void weeklySummaryReturnsNotFoundWhenRequesterIsOutsideCircle() throws Exception {
        User mainCaregiver = createUser("summary-outside-main", "Summary Outside Main");
        User outsideUser = createUser("summary-outside-user", "Summary Outside User");
        CareCircle careCircle = createCircleWithMember(mainCaregiver, CircleRole.MAIN_CAREGIVER, "Summary outside family");

        mockMvc.perform(get("/circles/{circleId}/summaries/weekly", careCircle.getId())
                        .with(jwt().jwt(token -> token
                                .subject(outsideUser.getSupabaseUserId())
                                .claim("email", outsideUser.getEmail())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void weeklySummaryRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/circles/{circleId}/summaries/weekly", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private void createOpenTask(CareCircle careCircle, User user, String title, OffsetDateTime dueAt) {
        CareTask task = new CareTask(careCircle, title, user);
        task.setDueAt(dueAt);
        careTaskRepository.save(task);
    }

    private void createCompletedTask(CareCircle careCircle, User user, String title, OffsetDateTime completedAt) {
        CareTask task = new CareTask(careCircle, title, user);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(completedAt);
        task.setCompletedByUser(user);
        careTaskRepository.save(task);
    }

    private CareCircle createCircleWithMember(User user, CircleRole role, String name) {
        CareCircle careCircle = careCircleRepository.save(new CareCircle(name, user));
        elderProfileRepository.save(new ElderProfile(careCircle, name + " Elder"));
        circleMemberRepository.save(new CircleMember(careCircle, user, role));
        return careCircle;
    }

    private OffsetDateTime atHour(LocalDate date, int hour) {
        return date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime();
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
