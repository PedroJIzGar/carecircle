package com.carecircle.api.tasks.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.shared.exception.ForbiddenOperationException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
import com.carecircle.api.tasks.dto.CreateTaskRequest;
import com.carecircle.api.tasks.dto.TaskResponse;
import com.carecircle.api.tasks.entity.CareTask;
import com.carecircle.api.tasks.entity.TaskPriority;
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
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static int getStatusOrder(com.carecircle.api.tasks.entity.TaskStatus status) {
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
