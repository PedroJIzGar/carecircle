package com.carecircle.api.appointments.dto;

import com.carecircle.api.appointments.entity.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a care circle appointment.
 *
 * @param id internal appointment identifier.
 * @param careCircleId care circle identifier.
 * @param title appointment title.
 * @param location optional appointment location or channel.
 * @param notes optional family coordination notes.
 * @param status current appointment lifecycle state.
 * @param startsAt appointment start date and time.
 * @param endsAt optional appointment end date and time.
 * @param createdByUserId internal user that created the appointment.
 * @param createdByFullName display name of the creator.
 * @param cancelledAt cancellation timestamp, when cancelled.
 * @param cancelledByUserId internal user that cancelled the appointment, when cancelled.
 * @param createdAt entity creation timestamp.
 * @param updatedAt last update timestamp.
 */
public record AppointmentResponse(
        UUID id,
        UUID careCircleId,
        String title,
        String location,
        String notes,
        AppointmentStatus status,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        UUID createdByUserId,
        String createdByFullName,
        OffsetDateTime cancelledAt,
        UUID cancelledByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
