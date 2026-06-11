package com.carecircle.api.appointments.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Request body for updating editable fields of a scheduled care circle appointment.
 *
 * <p>Null fields are treated as absent. Explicit clear flags are used for
 * optional fields that must support being reset without adding JSON Merge Patch
 * complexity to the MVP.</p>
 *
 * @param title optional replacement title.
 * @param location optional replacement location or channel.
 * @param notes optional replacement family coordination notes.
 * @param startsAt optional replacement start date and time.
 * @param endsAt optional replacement end date and time.
 * @param clearLocation true to remove the current location.
 * @param clearNotes true to remove the current notes.
 * @param clearEndsAt true to remove the current end date and time.
 */
public record UpdateAppointmentRequest(
        @Size(max = 160)
        String title,

        @Size(max = 255)
        String location,

        @Size(max = 1000)
        String notes,

        @FutureOrPresent
        OffsetDateTime startsAt,

        @FutureOrPresent
        OffsetDateTime endsAt,

        Boolean clearLocation,

        Boolean clearNotes,

        Boolean clearEndsAt
) {
}
