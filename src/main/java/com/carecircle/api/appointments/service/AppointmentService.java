package com.carecircle.api.appointments.service;

import com.carecircle.api.appointments.dto.AppointmentResponse;
import com.carecircle.api.appointments.dto.CreateAppointmentRequest;
import com.carecircle.api.appointments.dto.UpdateAppointmentRequest;
import com.carecircle.api.appointments.entity.Appointment;
import com.carecircle.api.appointments.entity.AppointmentStatus;
import com.carecircle.api.appointments.mapper.AppointmentMapper;
import com.carecircle.api.appointments.repository.AppointmentRepository;
import com.carecircle.api.auth.dto.SupabaseUserClaims;
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

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Application service for care circle appointment workflows.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final String CREATE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can create care circle appointments.";
    private static final String UPDATE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can update care circle appointments.";
    private static final String CANCEL_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can cancel care circle appointments.";
    private static final Comparator<Appointment> APPOINTMENT_LIST_ORDER = Comparator
            .comparingInt((Appointment appointment) -> getStatusOrder(appointment.getStatus()))
            .thenComparing(Appointment::getStartsAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Appointment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final UserService userService;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    /**
     * Creates a scheduled non-clinical appointment inside a care circle.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated appointment creation request.
     * @return created appointment response.
     */
    @Transactional
    public AppointmentResponse createAppointment(
            SupabaseUserClaims claims,
            UUID careCircleId,
            CreateAppointmentRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(CREATE_FORBIDDEN_MESSAGE);
        }

        validateTimeRange(request.startsAt(), request.endsAt());

        Appointment appointment = new Appointment(
                currentMembership.getCareCircle(),
                normalizeRequired(request.title(), "Appointment title must not be blank."),
                request.startsAt(),
                currentUser
        );
        appointment.setLocation(normalizeOptional(request.location()));
        appointment.setNotes(normalizeOptional(request.notes()));
        appointment.setEndsAt(request.endsAt());

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    /**
     * Lists appointments visible to an active care circle member.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return ordered appointment responses for the circle.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointments(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        return appointmentRepository.findByCareCircle_Id(careCircleId)
                .stream()
                .sorted(APPOINTMENT_LIST_ORDER)
                .map(appointmentMapper::toResponse)
                .toList();
    }

    /**
     * Updates editable fields of a scheduled appointment.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param appointmentId requested appointment identifier.
     * @param request validated appointment update request.
     * @return updated appointment response.
     */
    @Transactional
    public AppointmentResponse updateAppointment(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UUID appointmentId,
            UpdateAppointmentRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(UPDATE_FORBIDDEN_MESSAGE);
        }

        validateUpdateRequest(request);

        Appointment appointment = appointmentRepository.findByIdAndCareCircle_Id(appointmentId, careCircleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ResourceConflictException("Only scheduled appointments can be updated.");
        }

        OffsetDateTime nextStartsAt = request.startsAt() == null ? appointment.getStartsAt() : request.startsAt();
        OffsetDateTime nextEndsAt = resolveNextEndsAt(appointment, request);
        validateTimeRange(nextStartsAt, nextEndsAt);

        if (request.title() != null) {
            appointment.setTitle(normalizeRequired(request.title(), "Appointment title must not be blank."));
        }
        if (isTrue(request.clearLocation())) {
            appointment.setLocation(null);
        } else if (request.location() != null) {
            appointment.setLocation(normalizeOptional(request.location()));
        }
        if (isTrue(request.clearNotes())) {
            appointment.setNotes(null);
        } else if (request.notes() != null) {
            appointment.setNotes(normalizeOptional(request.notes()));
        }
        if (request.startsAt() != null) {
            appointment.setStartsAt(request.startsAt());
        }
        if (isTrue(request.clearEndsAt())) {
            appointment.setEndsAt(null);
        } else if (request.endsAt() != null) {
            appointment.setEndsAt(request.endsAt());
        }

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    /**
     * Cancels a scheduled appointment without deleting it.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param appointmentId requested appointment identifier.
     * @return cancelled appointment response.
     */
    @Transactional
    public AppointmentResponse cancelAppointment(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UUID appointmentId
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(CANCEL_FORBIDDEN_MESSAGE);
        }

        Appointment appointment = appointmentRepository.findByIdAndCareCircle_Id(appointmentId, careCircleId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ResourceConflictException("Only scheduled appointments can be cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(OffsetDateTime.now());
        appointment.setCancelledByUser(currentUser);

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    private OffsetDateTime resolveNextEndsAt(Appointment appointment, UpdateAppointmentRequest request) {
        if (isTrue(request.clearEndsAt())) {
            return null;
        }
        return request.endsAt() == null ? appointment.getEndsAt() : request.endsAt();
    }

    private void validateTimeRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new InvalidRequestException("endsAt must be after startsAt.");
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

    private void validateUpdateRequest(UpdateAppointmentRequest request) {
        if (isTrue(request.clearLocation()) && request.location() != null) {
            throw new InvalidRequestException("location cannot be set and cleared in the same request.");
        }
        if (isTrue(request.clearNotes()) && request.notes() != null) {
            throw new InvalidRequestException("notes cannot be set and cleared in the same request.");
        }
        if (isTrue(request.clearEndsAt()) && request.endsAt() != null) {
            throw new InvalidRequestException("endsAt cannot be set and cleared in the same request.");
        }
        if (!hasAnyUpdateField(request)) {
            throw new InvalidRequestException("At least one appointment field must be provided.");
        }
    }

    private boolean hasAnyUpdateField(UpdateAppointmentRequest request) {
        return request.title() != null
                || request.location() != null
                || request.notes() != null
                || request.startsAt() != null
                || request.endsAt() != null
                || isTrue(request.clearLocation())
                || isTrue(request.clearNotes())
                || isTrue(request.clearEndsAt());
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static int getStatusOrder(AppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> 0;
            case CANCELLED -> 1;
        };
    }
}
