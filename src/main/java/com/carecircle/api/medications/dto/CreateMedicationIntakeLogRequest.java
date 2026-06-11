package com.carecircle.api.medications.dto;

import com.carecircle.api.medications.entity.MedicationIntakeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request body for creating a manual medication intake log.
 *
 * @param reminderId required medication reminder identifier.
 * @param status required family-recorded intake status.
 * @param occurredAt optional time the log refers to. Defaults to now when absent.
 * @param note optional family coordination note.
 */
public record CreateMedicationIntakeLogRequest(
        @NotNull
        UUID reminderId,

        @NotNull
        MedicationIntakeStatus status,

        @PastOrPresent
        OffsetDateTime occurredAt,

        @Size(max = 1000)
        String note
) {
}
