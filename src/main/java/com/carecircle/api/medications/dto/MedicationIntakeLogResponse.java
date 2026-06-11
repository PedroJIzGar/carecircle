package com.carecircle.api.medications.dto;

import com.carecircle.api.medications.entity.MedicationIntakeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a manual medication intake log.
 *
 * @param id internal intake log identifier.
 * @param careCircleId care circle identifier.
 * @param reminderId medication reminder identifier.
 * @param medicationName family-entered medication name from the reminder.
 * @param status family-recorded intake status.
 * @param occurredAt time the log refers to.
 * @param note optional family coordination note.
 * @param recordedByUserId internal user that recorded the log.
 * @param recordedByFullName display name of the recording user.
 * @param createdAt entity creation timestamp.
 * @param updatedAt last update timestamp.
 */
public record MedicationIntakeLogResponse(
        UUID id,
        UUID careCircleId,
        UUID reminderId,
        String medicationName,
        MedicationIntakeStatus status,
        OffsetDateTime occurredAt,
        String note,
        UUID recordedByUserId,
        String recordedByFullName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
