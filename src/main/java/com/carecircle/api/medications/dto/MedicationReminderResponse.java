package com.carecircle.api.medications.dto;

import com.carecircle.api.medications.entity.MedicationReminderStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a family medication reminder.
 *
 * @param id internal medication reminder identifier.
 * @param careCircleId care circle identifier.
 * @param medicationName family-entered medication name.
 * @param dosageText optional family-entered dosage label.
 * @param scheduleText family-entered schedule label.
 * @param instructions optional family coordination instructions.
 * @param status current reminder lifecycle state.
 * @param startDate optional first date for the reminder.
 * @param endDate optional last date for the reminder.
 * @param createdByUserId internal user that created the reminder.
 * @param createdByFullName display name of the creator.
 * @param archivedAt archive timestamp, when archived.
 * @param archivedByUserId internal user that archived the reminder, when archived.
 * @param createdAt entity creation timestamp.
 * @param updatedAt last update timestamp.
 */
public record MedicationReminderResponse(
        UUID id,
        UUID careCircleId,
        String medicationName,
        String dosageText,
        String scheduleText,
        String instructions,
        MedicationReminderStatus status,
        LocalDate startDate,
        LocalDate endDate,
        UUID createdByUserId,
        String createdByFullName,
        OffsetDateTime archivedAt,
        UUID archivedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
