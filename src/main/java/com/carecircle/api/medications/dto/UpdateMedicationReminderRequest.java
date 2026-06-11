package com.carecircle.api.medications.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for updating editable fields of an active family medication reminder.
 *
 * <p>Null fields are treated as absent. Explicit clear flags are used for
 * optional fields that must support being reset without adding JSON Merge Patch
 * complexity to the MVP.</p>
 *
 * @param medicationName optional replacement medication name.
 * @param dosageText optional replacement dosage label.
 * @param scheduleText optional replacement schedule label.
 * @param instructions optional replacement family coordination instructions.
 * @param startDate optional replacement start date.
 * @param endDate optional replacement end date.
 * @param clearDosageText true to remove the dosage label.
 * @param clearInstructions true to remove the instructions.
 * @param clearStartDate true to remove the start date.
 * @param clearEndDate true to remove the end date.
 */
public record UpdateMedicationReminderRequest(
        @Size(max = 160)
        String medicationName,

        @Size(max = 160)
        String dosageText,

        @Size(max = 255)
        String scheduleText,

        @Size(max = 1000)
        String instructions,

        LocalDate startDate,

        LocalDate endDate,

        Boolean clearDosageText,

        Boolean clearInstructions,

        Boolean clearStartDate,

        Boolean clearEndDate
) {
}
