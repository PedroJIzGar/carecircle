package com.carecircle.api.summaries.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Weekly non-clinical care circle summary.
 *
 * @param careCircleId care circle identifier.
 * @param weekStart Monday that starts the summarized week.
 * @param weekEnd Sunday that ends the summarized week.
 * @param generatedAt generation timestamp.
 * @param tasks task counters.
 * @param appointments appointment counters.
 * @param checkIns check-in counters.
 * @param medications medication intake counters.
 */
public record WeeklySummaryResponse(
        UUID careCircleId,
        LocalDate weekStart,
        LocalDate weekEnd,
        OffsetDateTime generatedAt,
        TaskWeeklySummaryResponse tasks,
        AppointmentWeeklySummaryResponse appointments,
        CheckInWeeklySummaryResponse checkIns,
        MedicationWeeklySummaryResponse medications
) {
}
