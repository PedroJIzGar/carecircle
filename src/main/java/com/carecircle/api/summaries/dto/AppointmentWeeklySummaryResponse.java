package com.carecircle.api.summaries.dto;

/**
 * Weekly appointment counters for a care circle.
 *
 * @param scheduledAppointmentsThisWeek scheduled appointments starting inside the week.
 * @param cancelledAppointmentsThisWeek appointments cancelled inside the week.
 */
public record AppointmentWeeklySummaryResponse(
        long scheduledAppointmentsThisWeek,
        long cancelledAppointmentsThisWeek
) {
}
