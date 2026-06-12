package com.carecircle.api.summaries.dto;

/**
 * Weekly task counters for a care circle.
 *
 * @param openTasksDueThisWeek open tasks with a due date inside the week.
 * @param completedTasksThisWeek tasks completed inside the week.
 * @param overdueOpenTasks open tasks due before the summary generation time.
 */
public record TaskWeeklySummaryResponse(
        long openTasksDueThisWeek,
        long completedTasksThisWeek,
        long overdueOpenTasks
) {
}
