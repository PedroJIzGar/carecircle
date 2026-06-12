package com.carecircle.api.summaries.service;

import com.carecircle.api.appointments.entity.Appointment;
import com.carecircle.api.appointments.entity.AppointmentStatus;
import com.carecircle.api.appointments.repository.AppointmentRepository;
import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.checkins.entity.CheckIn;
import com.carecircle.api.checkins.entity.CheckInStatus;
import com.carecircle.api.checkins.repository.CheckInRepository;
import com.carecircle.api.medications.entity.MedicationIntakeLog;
import com.carecircle.api.medications.entity.MedicationIntakeStatus;
import com.carecircle.api.medications.repository.MedicationIntakeLogRepository;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.shared.exception.InvalidRequestException;
import com.carecircle.api.summaries.dto.AppointmentWeeklySummaryResponse;
import com.carecircle.api.summaries.dto.CheckInWeeklySummaryResponse;
import com.carecircle.api.summaries.dto.MedicationWeeklySummaryResponse;
import com.carecircle.api.summaries.dto.TaskWeeklySummaryResponse;
import com.carecircle.api.summaries.dto.WeeklySummaryResponse;
import com.carecircle.api.tasks.entity.CareTask;
import com.carecircle.api.tasks.entity.TaskStatus;
import com.carecircle.api.tasks.repository.CareTaskRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Builds non-clinical weekly summaries from existing care circle records.
 */
@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final UserService userService;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final CareTaskRepository careTaskRepository;
    private final AppointmentRepository appointmentRepository;
    private final CheckInRepository checkInRepository;
    private final MedicationIntakeLogRepository medicationIntakeLogRepository;

    /**
     * Builds a weekly summary for an active care circle member.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param requestedWeekStart optional requested Monday. Defaults to current week Monday.
     * @return weekly summary counters.
     */
    @Transactional(readOnly = true)
    public WeeklySummaryResponse getWeeklySummary(
            SupabaseUserClaims claims,
            UUID careCircleId,
            LocalDate requestedWeekStart
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        LocalDate weekStart = resolveWeekStart(requestedWeekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        OffsetDateTime startInclusive = startOfDay(weekStart);
        OffsetDateTime endExclusive = startOfDay(weekStart.plusDays(7));
        OffsetDateTime generatedAt = OffsetDateTime.now();

        return new WeeklySummaryResponse(
                careCircleId,
                weekStart,
                weekEnd,
                generatedAt,
                summarizeTasks(careCircleId, startInclusive, endExclusive, generatedAt),
                summarizeAppointments(careCircleId, startInclusive, endExclusive),
                summarizeCheckIns(careCircleId, startInclusive, endExclusive),
                summarizeMedications(careCircleId, startInclusive, endExclusive)
        );
    }

    private LocalDate resolveWeekStart(LocalDate requestedWeekStart) {
        LocalDate weekStart = requestedWeekStart == null
                ? LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : requestedWeekStart;

        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new InvalidRequestException("weekStart must be a Monday.");
        }
        return weekStart;
    }

    private TaskWeeklySummaryResponse summarizeTasks(
            UUID careCircleId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive,
            OffsetDateTime generatedAt
    ) {
        List<CareTask> tasks = careTaskRepository.findByCareCircle_Id(careCircleId);
        long openTasksDueThisWeek = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.OPEN)
                .filter(task -> isWithinWeek(task.getDueAt(), startInclusive, endExclusive))
                .count();
        long completedTasksThisWeek = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .filter(task -> isWithinWeek(task.getCompletedAt(), startInclusive, endExclusive))
                .count();
        long overdueOpenTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.OPEN)
                .filter(task -> task.getDueAt() != null && task.getDueAt().isBefore(generatedAt))
                .count();

        return new TaskWeeklySummaryResponse(openTasksDueThisWeek, completedTasksThisWeek, overdueOpenTasks);
    }

    private AppointmentWeeklySummaryResponse summarizeAppointments(
            UUID careCircleId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        List<Appointment> appointments = appointmentRepository.findByCareCircle_Id(careCircleId);
        long scheduledAppointmentsThisWeek = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.SCHEDULED)
                .filter(appointment -> isWithinWeek(appointment.getStartsAt(), startInclusive, endExclusive))
                .count();
        long cancelledAppointmentsThisWeek = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CANCELLED)
                .filter(appointment -> isWithinWeek(appointment.getCancelledAt(), startInclusive, endExclusive))
                .count();

        return new AppointmentWeeklySummaryResponse(scheduledAppointmentsThisWeek, cancelledAppointmentsThisWeek);
    }

    private CheckInWeeklySummaryResponse summarizeCheckIns(
            UUID careCircleId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        List<CheckIn> checkIns = checkInRepository.findByCareCircle_Id(careCircleId).stream()
                .filter(checkIn -> isWithinWeek(checkIn.getCheckedAt(), startInclusive, endExclusive))
                .toList();

        long okCheckIns = countCheckInsByStatus(checkIns, CheckInStatus.OK);
        long needsAttentionCheckIns = countCheckInsByStatus(checkIns, CheckInStatus.NEEDS_ATTENTION);
        long noResponseCheckIns = countCheckInsByStatus(checkIns, CheckInStatus.NO_RESPONSE);

        return new CheckInWeeklySummaryResponse(
                checkIns.size(),
                okCheckIns,
                needsAttentionCheckIns,
                noResponseCheckIns
        );
    }

    private MedicationWeeklySummaryResponse summarizeMedications(
            UUID careCircleId,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        List<MedicationIntakeLog> intakeLogs = medicationIntakeLogRepository.findByCareCircle_Id(careCircleId).stream()
                .filter(log -> isWithinWeek(log.getOccurredAt(), startInclusive, endExclusive))
                .toList();

        long takenLogs = countMedicationLogsByStatus(intakeLogs, MedicationIntakeStatus.TAKEN);
        long skippedLogs = countMedicationLogsByStatus(intakeLogs, MedicationIntakeStatus.SKIPPED);

        return new MedicationWeeklySummaryResponse(intakeLogs.size(), takenLogs, skippedLogs);
    }

    private long countCheckInsByStatus(List<CheckIn> checkIns, CheckInStatus status) {
        return checkIns.stream()
                .filter(checkIn -> checkIn.getStatus() == status)
                .count();
    }

    private long countMedicationLogsByStatus(List<MedicationIntakeLog> intakeLogs, MedicationIntakeStatus status) {
        return intakeLogs.stream()
                .filter(log -> log.getStatus() == status)
                .count();
    }

    private boolean isWithinWeek(OffsetDateTime value, OffsetDateTime startInclusive, OffsetDateTime endExclusive) {
        return value != null && !value.isBefore(startInclusive) && value.isBefore(endExclusive);
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
