package com.carecircle.api.medications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating a family medication reminder.
 *
 * @param medicationName required family-entered medication name.
 * @param dosageText optional family-entered dosage label.
 * @param scheduleText required family-entered schedule label.
 * @param instructions optional family coordination instructions.
 * @param startDate optional first date for the reminder.
 * @param endDate optional last date for the reminder.
 */
public record CreateMedicationReminderRequest(
        @NotBlank
        @Size(max = 160)
        String medicationName,

        @Size(max = 160)
        String dosageText,

        @NotBlank
        @Size(max = 255)
        String scheduleText,

        @Size(max = 1000)
        String instructions,

        LocalDate startDate,

        LocalDate endDate
) {
}
