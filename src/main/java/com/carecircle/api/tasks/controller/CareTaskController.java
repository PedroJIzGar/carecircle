package com.carecircle.api.tasks.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.tasks.dto.CreateTaskRequest;
import com.carecircle.api.tasks.dto.TaskResponse;
import com.carecircle.api.tasks.service.CareTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Care circle task API endpoints.
 */
@RestController
@RequestMapping("/circles/{circleId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Care circle task coordination endpoints")
public class CareTaskController {

    private final CurrentUserProvider currentUserProvider;
    private final CareTaskService careTaskService;

    /**
     * Creates a non-clinical coordination task in a care circle.
     *
     * @param circleId requested care circle identifier.
     * @param request validated task creation request.
     * @return created task response.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a care circle task",
            description = "Creates an OPEN non-clinical coordination task when the authenticated user is MAIN_CAREGIVER or COLLABORATOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public TaskResponse createTask(
            @PathVariable UUID circleId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return careTaskService.createTask(claims, circleId, request);
    }

    /**
     * Lists tasks visible to the authenticated member of a care circle.
     *
     * @param circleId requested care circle identifier.
     * @return ordered tasks for the requested circle.
     */
    @GetMapping
    @Operation(
            summary = "List care circle tasks",
            description = "Returns tasks when the authenticated user is an active member of the care circle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<TaskResponse> listTasks(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return careTaskService.listTasks(claims, circleId);
    }
}
