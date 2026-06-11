package com.carecircle.api.tasks.mapper;

import com.carecircle.api.tasks.dto.TaskResponse;
import com.carecircle.api.tasks.entity.CareTask;
import com.carecircle.api.users.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps task entities to API DTOs.
 */
@Component
public class CareTaskMapper {

    /**
     * Converts a persisted task into a response safe for care circle members.
     *
     * @param task persisted care circle task.
     * @return public task response.
     */
    public TaskResponse toResponse(CareTask task) {
        User assignedToUser = task.getAssignedToUser();
        User createdByUser = task.getCreatedByUser();
        User completedByUser = task.getCompletedByUser();

        return new TaskResponse(
                task.getId(),
                task.getCareCircle().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueAt(),
                getUserId(assignedToUser),
                getFullName(assignedToUser),
                createdByUser.getId(),
                createdByUser.getFullName(),
                task.getCompletedAt(),
                getUserId(completedByUser),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private UUID getUserId(User user) {
        return user == null ? null : user.getId();
    }

    private String getFullName(User user) {
        return user == null ? null : user.getFullName();
    }
}
