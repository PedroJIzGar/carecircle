package com.carecircle.api.tasks.dto;

import com.carecircle.api.tasks.entity.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request body for creating a care circle task.
 *
 * @param title required short task title.
 * @param description optional family coordination details.
 * @param priority optional priority. Defaults to {@code NORMAL} when absent.
 * @param dueAt optional due date and time.
 * @param assignedToUserId optional internal user id for an active member of the circle.
 */
public record CreateTaskRequest(
        @NotBlank
        @Size(max = 160)
        String title,

        @Size(max = 1000)
        String description,

        TaskPriority priority,

        @FutureOrPresent
        OffsetDateTime dueAt,

        UUID assignedToUserId
) {
}
