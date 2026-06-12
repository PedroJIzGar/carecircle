package com.carecircle.api.summaries.dto;

/**
 * Weekly check-in counters for a care circle.
 *
 * @param totalCheckIns total check-ins recorded inside the week.
 * @param okCheckIns check-ins with {@code OK} status.
 * @param needsAttentionCheckIns check-ins with {@code NEEDS_ATTENTION} status.
 * @param noResponseCheckIns check-ins with {@code NO_RESPONSE} status.
 */
public record CheckInWeeklySummaryResponse(
        long totalCheckIns,
        long okCheckIns,
        long needsAttentionCheckIns,
        long noResponseCheckIns
) {
}
