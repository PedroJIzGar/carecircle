package com.carecircle.api.medications.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.medications.dto.CreateMedicationIntakeLogRequest;
import com.carecircle.api.medications.dto.CreateMedicationReminderRequest;
import com.carecircle.api.medications.dto.MedicationIntakeLogResponse;
import com.carecircle.api.medications.dto.MedicationReminderResponse;
import com.carecircle.api.medications.dto.UpdateMedicationReminderRequest;
import com.carecircle.api.medications.entity.MedicationIntakeLog;
import com.carecircle.api.medications.entity.MedicationReminder;
import com.carecircle.api.medications.entity.MedicationReminderStatus;
import com.carecircle.api.medications.mapper.MedicationIntakeLogMapper;
import com.carecircle.api.medications.mapper.MedicationReminderMapper;
import com.carecircle.api.medications.repository.MedicationIntakeLogRepository;
import com.carecircle.api.medications.repository.MedicationReminderRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.shared.exception.ForbiddenOperationException;
import com.carecircle.api.shared.exception.InvalidRequestException;
import com.carecircle.api.shared.exception.ResourceConflictException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Application service for family medication reminder and intake log workflows.
 */
@Service
@RequiredArgsConstructor
public class MedicationService {

    private static final String WRITE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can manage care circle medication reminders.";
    private static final String LOG_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can record medication intake logs.";
    private static final Comparator<MedicationReminder> REMINDER_LIST_ORDER = Comparator
            .comparingInt((MedicationReminder reminder) -> getStatusOrder(reminder.getStatus()))
            .thenComparing(MedicationReminder::getMedicationName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(MedicationReminder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    private static final Comparator<MedicationIntakeLog> INTAKE_LOG_LIST_ORDER = Comparator
            .comparing(MedicationIntakeLog::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MedicationIntakeLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final UserService userService;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final MedicationReminderRepository medicationReminderRepository;
    private final MedicationIntakeLogRepository medicationIntakeLogRepository;
    private final MedicationReminderMapper medicationReminderMapper;
    private final MedicationIntakeLogMapper medicationIntakeLogMapper;

    /**
     * Creates an active family medication reminder inside a care circle.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated reminder creation request.
     * @return created medication reminder response.
     */
    @Transactional
    public MedicationReminderResponse createReminder(
            SupabaseUserClaims claims,
            UUID careCircleId,
            CreateMedicationReminderRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = getWritableMembership(careCircleId, currentUser, WRITE_FORBIDDEN_MESSAGE);
        validateDateRange(request.startDate(), request.endDate());

        MedicationReminder reminder = new MedicationReminder(
                currentMembership.getCareCircle(),
                normalizeRequired(request.medicationName(), "Medication name must not be blank."),
                normalizeRequired(request.scheduleText(), "Medication schedule must not be blank."),
                currentUser
        );
        reminder.setDosageText(normalizeOptional(request.dosageText()));
        reminder.setInstructions(normalizeOptional(request.instructions()));
        reminder.setStartDate(request.startDate());
        reminder.setEndDate(request.endDate());

        return medicationReminderMapper.toResponse(medicationReminderRepository.save(reminder));
    }

    /**
     * Lists medication reminders visible to an active care circle member.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return ordered medication reminders for the circle.
     */
    @Transactional(readOnly = true)
    public List<MedicationReminderResponse> listReminders(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        return medicationReminderRepository.findByCareCircle_Id(careCircleId)
                .stream()
                .sorted(REMINDER_LIST_ORDER)
                .map(medicationReminderMapper::toResponse)
                .toList();
    }

    /**
     * Updates editable fields of an active family medication reminder.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param reminderId requested reminder identifier.
     * @param request validated reminder update request.
     * @return updated medication reminder response.
     */
    @Transactional
    public MedicationReminderResponse updateReminder(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UUID reminderId,
            UpdateMedicationReminderRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        getWritableMembership(careCircleId, currentUser, WRITE_FORBIDDEN_MESSAGE);
        validateUpdateRequest(request);

        MedicationReminder reminder = getReminderOrThrow(reminderId, careCircleId);
        if (reminder.getStatus() != MedicationReminderStatus.ACTIVE) {
            throw new ResourceConflictException("Only active medication reminders can be updated.");
        }

        LocalDate nextStartDate = isTrue(request.clearStartDate()) ? null
                : request.startDate() == null ? reminder.getStartDate() : request.startDate();
        LocalDate nextEndDate = isTrue(request.clearEndDate()) ? null
                : request.endDate() == null ? reminder.getEndDate() : request.endDate();
        validateDateRange(nextStartDate, nextEndDate);

        if (request.medicationName() != null) {
            reminder.setMedicationName(normalizeRequired(
                    request.medicationName(),
                    "Medication name must not be blank."
            ));
        }
        if (request.scheduleText() != null) {
            reminder.setScheduleText(normalizeRequired(
                    request.scheduleText(),
                    "Medication schedule must not be blank."
            ));
        }
        if (isTrue(request.clearDosageText())) {
            reminder.setDosageText(null);
        } else if (request.dosageText() != null) {
            reminder.setDosageText(normalizeOptional(request.dosageText()));
        }
        if (isTrue(request.clearInstructions())) {
            reminder.setInstructions(null);
        } else if (request.instructions() != null) {
            reminder.setInstructions(normalizeOptional(request.instructions()));
        }
        if (isTrue(request.clearStartDate())) {
            reminder.setStartDate(null);
        } else if (request.startDate() != null) {
            reminder.setStartDate(request.startDate());
        }
        if (isTrue(request.clearEndDate())) {
            reminder.setEndDate(null);
        } else if (request.endDate() != null) {
            reminder.setEndDate(request.endDate());
        }

        return medicationReminderMapper.toResponse(medicationReminderRepository.save(reminder));
    }

    /**
     * Archives an active family medication reminder without deleting its history.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param reminderId requested reminder identifier.
     * @return archived medication reminder response.
     */
    @Transactional
    public MedicationReminderResponse archiveReminder(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UUID reminderId
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        getWritableMembership(careCircleId, currentUser, WRITE_FORBIDDEN_MESSAGE);

        MedicationReminder reminder = getReminderOrThrow(reminderId, careCircleId);
        if (reminder.getStatus() != MedicationReminderStatus.ACTIVE) {
            throw new ResourceConflictException("Only active medication reminders can be archived.");
        }

        reminder.setStatus(MedicationReminderStatus.ARCHIVED);
        reminder.setArchivedAt(OffsetDateTime.now());
        reminder.setArchivedByUser(currentUser);

        return medicationReminderMapper.toResponse(medicationReminderRepository.save(reminder));
    }

    /**
     * Creates a manual intake log for an active medication reminder.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated intake log creation request.
     * @return created intake log response.
     */
    @Transactional
    public MedicationIntakeLogResponse createIntakeLog(
            SupabaseUserClaims claims,
            UUID careCircleId,
            CreateMedicationIntakeLogRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = getWritableMembership(careCircleId, currentUser, LOG_FORBIDDEN_MESSAGE);
        MedicationReminder reminder = getReminderOrThrow(request.reminderId(), careCircleId);
        if (reminder.getStatus() != MedicationReminderStatus.ACTIVE) {
            throw new ResourceConflictException("Only active medication reminders can receive intake logs.");
        }

        MedicationIntakeLog intakeLog = new MedicationIntakeLog(
                currentMembership.getCareCircle(),
                reminder,
                request.status(),
                resolveOccurredAt(request.occurredAt()),
                currentUser
        );
        intakeLog.setNote(normalizeOptional(request.note()));

        return medicationIntakeLogMapper.toResponse(medicationIntakeLogRepository.save(intakeLog));
    }

    /**
     * Lists intake logs visible to an active care circle member.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return ordered medication intake logs for the circle.
     */
    @Transactional(readOnly = true)
    public List<MedicationIntakeLogResponse> listIntakeLogs(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        return medicationIntakeLogRepository.findByCareCircle_Id(careCircleId)
                .stream()
                .sorted(INTAKE_LOG_LIST_ORDER)
                .map(medicationIntakeLogMapper::toResponse)
                .toList();
    }

    private MedicationReminder getReminderOrThrow(UUID reminderId, UUID careCircleId) {
        return medicationReminderRepository.findByIdAndCareCircle_Id(reminderId, careCircleId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication reminder not found."));
    }

    private CircleMember getWritableMembership(UUID careCircleId, User currentUser, String forbiddenMessage) {
        CircleMember membership = circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);
        if (membership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(forbiddenMessage);
        }
        return membership;
    }

    private OffsetDateTime resolveOccurredAt(OffsetDateTime requestedOccurredAt) {
        OffsetDateTime occurredAt = requestedOccurredAt == null ? OffsetDateTime.now() : requestedOccurredAt;
        if (occurredAt.isAfter(OffsetDateTime.now())) {
            throw new InvalidRequestException("occurredAt must not be in the future.");
        }
        return occurredAt;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidRequestException("endDate must be on or after startDate.");
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new InvalidRequestException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateUpdateRequest(UpdateMedicationReminderRequest request) {
        if (isTrue(request.clearDosageText()) && request.dosageText() != null) {
            throw new InvalidRequestException("dosageText cannot be set and cleared in the same request.");
        }
        if (isTrue(request.clearInstructions()) && request.instructions() != null) {
            throw new InvalidRequestException("instructions cannot be set and cleared in the same request.");
        }
        if (isTrue(request.clearStartDate()) && request.startDate() != null) {
            throw new InvalidRequestException("startDate cannot be set and cleared in the same request.");
        }
        if (isTrue(request.clearEndDate()) && request.endDate() != null) {
            throw new InvalidRequestException("endDate cannot be set and cleared in the same request.");
        }
        if (!hasAnyUpdateField(request)) {
            throw new InvalidRequestException("At least one medication reminder field must be provided.");
        }
    }

    private boolean hasAnyUpdateField(UpdateMedicationReminderRequest request) {
        return request.medicationName() != null
                || request.dosageText() != null
                || request.scheduleText() != null
                || request.instructions() != null
                || request.startDate() != null
                || request.endDate() != null
                || isTrue(request.clearDosageText())
                || isTrue(request.clearInstructions())
                || isTrue(request.clearStartDate())
                || isTrue(request.clearEndDate());
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static int getStatusOrder(MedicationReminderStatus status) {
        return switch (status) {
            case ACTIVE -> 0;
            case ARCHIVED -> 1;
        };
    }
}
