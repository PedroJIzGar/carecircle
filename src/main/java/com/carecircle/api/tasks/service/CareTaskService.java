package com.carecircle.api.tasks.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.shared.exception.ForbiddenOperationException;
import com.carecircle.api.shared.exception.InvalidRequestException;
import com.carecircle.api.shared.exception.ResourceConflictException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
import com.carecircle.api.tasks.dto.CreateTaskRequest;
import com.carecircle.api.tasks.dto.TaskResponse;
import com.carecircle.api.tasks.dto.UpdateTaskRequest;
import com.carecircle.api.tasks.entity.CareTask;
import com.carecircle.api.tasks.entity.TaskPriority;
import com.carecircle.api.tasks.entity.TaskStatus;
import com.carecircle.api.tasks.mapper.CareTaskMapper;
import com.carecircle.api.tasks.repository.CareTaskRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Application service for care circle task workflows.
 */
@Service
@RequiredArgsConstructor
public class CareTaskService {

    private static final String CREATE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can create care circle tasks.";
    private static final String UPDATE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can update care circle tasks.";
    private static final String COMPLETE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can complete care circle tasks.";
    private static final String CANCEL_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can cancel care circle tasks.";
    private static final Comparator<CareTask> TASK_LIST_ORDER = Comparator
            .comparingInt((CareTask task) -> getStatusOrder(task.getStatus()))
            .thenComparing(CareTaskService::getDueAtBucket)
            .thenComparing(CareTask::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(CareTask::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final UserService userService;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final CircleMemberRepository circleMemberRepository;
    private final CareTaskRepository careTaskRepository;
    private final CareTaskMapper careTaskMapper;

    /**
     * Creates an open non-clinical coordination task inside a care circle.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated task creation request.
     * @return created task response.
     */
    @Transactional
    public TaskResponse createTask(SupabaseUserClaims claims, UUID careCircleId, CreateTaskRequest request) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(CREATE_FORBIDDEN_MESSAGE);
        }

        CareTask task = new CareTask(
                currentMembership.getCareCircle(),
                normalizeRequired(request.title()),
                currentUser
        );
        task.setDescription(normalizeOptional(request.description()));
        task.setPriority(request.priority() == null ? TaskPriority.NORMAL : request.priority());
        task.setDueAt(request.dueAt());
        task.setAssignedToUser(resolveAssignedUser(careCircleId, request.assignedToUserId()));

        return careTaskMapper.toResponse(careTaskRepository.save(task));
    }

    /**
     * Lists tasks visible to an active care circle member.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return ordered task responses for the circle.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        return careTaskRepository.findByCareCircle_Id(careCircleId)
                .stream()
                .sorted(TASK_LIST_ORDER)
                .map(careTaskMapper::toResponse)
                .toList();
    }

    /**
     * Updates editable fields of an open task.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param taskId requested task identifier.
     * @param request validated task update request.
     * @return updated task response.
     */
    @Transactional
    public TaskResponse updateTask(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UUID taskId,
            UpdateTaskRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(UPDATE_FORBIDDEN_MESSAGE);
        }

        validateUpdateRequest(request);

        CareTask task = careTaskRepository.findByIdAndCareCircle_Id(taskId, careCircleId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (task.getStatus() != TaskStatus.OPEN) {
            throw new ResourceConflictException("Only open tasks can be updated.");
        }

        if (request.title() != null) {
            task.setTitle(normalizeRequired(request.title()));
        }
        if (isTrue(request.clearDescription())) {
            task.setDescription(null);
        } else if (request.description() != null) {
            task.setDescription(normalizeOptional(request.description()));
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (isTrue(request.clearDueAt())) {
            task.setDueAt(null);
        } else if (request.dueAt() != null) {
            task.setDueAt(request.dueAt());
        }
        if (isTrue(request.clearAssignment())) {
            task.setAssignedToUser(null);
        } else if (request.assignedToUserId() != null) {
            task.setAssignedToUser(resolveAssignedUser(careCircleId, request.assignedToUserId()));
        }

        return careTaskMapper.toResponse(careTaskRepository.save(task));
    }

    /**
     * Completes an open task and records who completed it.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param taskId requested task identifier.
     * @return completed task response.
     */
    @Transactional
    public TaskResponse completeTask(SupabaseUserClaims claims, UUID careCircleId, UUID taskId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(COMPLETE_FORBIDDEN_MESSAGE);
        }

        CareTask task = careTaskRepository.findByIdAndCareCircle_Id(taskId, careCircleId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (task.getStatus() != TaskStatus.OPEN) {
            throw new ResourceConflictException("Only open tasks can be completed.");
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(OffsetDateTime.now());
        task.setCompletedByUser(currentUser);

        return careTaskMapper.toResponse(careTaskRepository.save(task));
    }

    /**
     * Cancels an open task without deleting it.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param taskId requested task identifier.
     * @return cancelled task response.
     */
    @Transactional
    public TaskResponse cancelTask(SupabaseUserClaims claims, UUID careCircleId, UUID taskId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(CANCEL_FORBIDDEN_MESSAGE);
        }

        CareTask task = careTaskRepository.findByIdAndCareCircle_Id(taskId, careCircleId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (task.getStatus() != TaskStatus.OPEN) {
            throw new ResourceConflictException("Only open tasks can be cancelled.");
        }

        task.setStatus(TaskStatus.CANCELLED);

        return careTaskMapper.toResponse(careTaskRepository.save(task));
    }

    private User resolveAssignedUser(UUID careCircleId, UUID assignedToUserId) {
        if (assignedToUserId == null) {
            return null;
        }

        return circleMemberRepository.findByCareCircle_IdAndUser_IdAndStatus(
                        careCircleId,
                        assignedToUserId,
                        CircleMemberStatus.ACTIVE
                )
                .map(CircleMember::getUser)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found in care circle."));
    }

    private String normalizeRequired(String value) {
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new InvalidRequestException("Task title must not be blank.");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateUpdateRequest(UpdateTaskRequest request) {
        if (isTrue(request.clearDescription()) && request.description() != null) {
            throw new InvalidRequestException("description cannot be set and cleared in the same request.");
        }
        if (isTrue(request.clearDueAt()) && request.dueAt() != null) {
            throw new InvalidRequestException("dueAt cannot be set and cleared in the same request.");
        }
        if (isTrue(request.clearAssignment()) && request.assignedToUserId() != null) {
            throw new InvalidRequestException("assignedToUserId cannot be set and cleared in the same request.");
        }
        if (!hasAnyUpdateField(request)) {
            throw new InvalidRequestException("At least one task field must be provided.");
        }
    }

    private boolean hasAnyUpdateField(UpdateTaskRequest request) {
        return request.title() != null
                || request.description() != null
                || request.priority() != null
                || request.dueAt() != null
                || request.assignedToUserId() != null
                || isTrue(request.clearDescription())
                || isTrue(request.clearDueAt())
                || isTrue(request.clearAssignment());
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static int getStatusOrder(TaskStatus status) {
        return switch (status) {
            case OPEN -> 0;
            case COMPLETED -> 1;
            case CANCELLED -> 2;
        };
    }

    private static int getDueAtBucket(CareTask task) {
        OffsetDateTime dueAt = task.getDueAt();
        return dueAt == null ? 1 : 0;
    }
}
