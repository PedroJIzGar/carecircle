package com.carecircle.api.appointments.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Request body for creating a care circle appointment.
 *
 * @param title required short appointment title.
 * @param location optional appointment location or channel.
 * @param notes optional family coordination notes.
 * @param startsAt required appointment start date and time.
 * @param endsAt optional appointment end date and time.
 */
public record CreateAppointmentRequest(
        @NotBlank
        @Size(max = 160)
        String title,

        @Size(max = 255)
        String location,

        @Size(max = 1000)
        String notes,

        @NotNull
        @FutureOrPresent
        OffsetDateTime startsAt,

        @FutureOrPresent
        OffsetDateTime endsAt
) {
}
