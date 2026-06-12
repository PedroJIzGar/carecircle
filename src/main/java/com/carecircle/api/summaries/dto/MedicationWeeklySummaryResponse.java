package com.carecircle.api.summaries.dto;

/**
 * Weekly medication intake counters for a care circle.
 *
 * @param intakeLogsThisWeek manual intake logs recorded for the week.
 * @param takenLogs logs with {@code TAKEN} status.
 * @param skippedLogs logs with {@code SKIPPED} status.
 */
public record MedicationWeeklySummaryResponse(
        long intakeLogsThisWeek,
        long takenLogs,
        long skippedLogs
) {
}
