package com.carecircle.api.tasks.dto;

import com.carecircle.api.tasks.entity.TaskPriority;
import com.carecircle.api.tasks.entity.TaskStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public API representation of a care circle task.
 *
 * <p>The response exposes internal CareCircle user identifiers for members
 * already visible inside the circle, but does not expose identity-provider ids.</p>
 *
 * @param id task identifier.
 * @param careCircleId owning care circle identifier.
 * @param title task title.
 * @param description optional task details.
 * @param status current task status.
 * @param priority task priority.
 * @param dueAt optional due date and time.
 * @param assignedToUserId optional assigned user identifier.
 * @param assignedToFullName optional assigned user display name.
 * @param createdByUserId user that created the task.
 * @param createdByFullName optional creator display name.
 * @param completedAt optional completion timestamp.
 * @param completedByUserId optional completing user identifier.
 * @param createdAt task creation timestamp.
 * @param updatedAt task update timestamp.
 */
public record TaskResponse(
        UUID id,
        UUID careCircleId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        OffsetDateTime dueAt,
        UUID assignedToUserId,
        String assignedToFullName,
        UUID createdByUserId,
        String createdByFullName,
        OffsetDateTime completedAt,
        UUID completedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
