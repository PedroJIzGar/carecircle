package com.carecircle.api.tasks.dto;

import com.carecircle.api.tasks.entity.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request body for updating editable fields of an open care circle task.
 *
 * <p>Null fields are treated as absent. Explicit clear flags are used for
 * optional fields that must support being reset without adding JSON Merge Patch
 * complexity to the MVP.</p>
 *
 * @param title optional replacement title.
 * @param description optional replacement description.
 * @param priority optional replacement priority.
 * @param dueAt optional replacement due date and time.
 * @param assignedToUserId optional replacement assigned user id.
 * @param clearDescription true to remove the current description.
 * @param clearDueAt true to remove the current due date.
 * @param clearAssignment true to remove the current assignment.
 */
public record UpdateTaskRequest(
        @Size(max = 160)
        String title,

        @Size(max = 1000)
        String description,

        TaskPriority priority,

        @FutureOrPresent
        OffsetDateTime dueAt,

        UUID assignedToUserId,

        Boolean clearDescription,

        Boolean clearDueAt,

        Boolean clearAssignment
) {
}
