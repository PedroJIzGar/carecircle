# Step 9D - Complete Care Circle Task

## Goal

Add the task completion endpoint:

```http
POST /api/circles/{circleId}/tasks/{taskId}/complete
```

This marks an open task as completed and records who completed it.

## Technical Decision

Task completion is modeled as an explicit action endpoint, not as a generic
field update.

That keeps the workflow readable:

- `PATCH /tasks/{taskId}` edits task details.
- `POST /tasks/{taskId}/complete` changes task lifecycle state.

Completion writes:

- `status = COMPLETED`
- `completedAt = now`
- `completedByUser = current authenticated user`

## Authorization Rules

- A valid Supabase Bearer token is required.
- The requester must be an active member of the care circle.
- `MAIN_CAREGIVER` can complete tasks.
- `COLLABORATOR` can complete tasks.
- `OBSERVER` cannot complete tasks.
- The task must belong to the requested care circle.
- Only `OPEN` tasks can be completed.

## Files Changed

- `src/main/java/com/carecircle/api/tasks/controller/CareTaskController.java`
- `src/main/java/com/carecircle/api/tasks/service/CareTaskService.java`
- `src/test/java/com/carecircle/api/tasks/CareTaskControllerTests.java`

## Manual Test

Use a token from a `MAIN_CAREGIVER` or `COLLABORATOR`:

```powershell
curl.exe -X POST "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks/TU_TASK_ID/complete" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

Expected result:

```http
200 OK
```

The response should contain:

- `status: "COMPLETED"`
- `completedAt`
- `completedByUserId`

## Expected Errors

- `401 Unauthorized`: missing, expired, or invalid Bearer token.
- `403 Forbidden`: requester is an `OBSERVER`.
- `404 Not Found`: requester is outside the circle or task belongs to another circle.
- `409 Conflict`: task is not `OPEN`.

## Automated Test

```powershell
.\mvnw.cmd clean test
```

The tests cover:

- successful completion by `COLLABORATOR`
- completed state persisted in database
- rejection for `OBSERVER`
- hidden resources for outside users
- hidden tasks from other circles
- conflict when task is already completed
- authentication requirement

## Next Step

Step 9E should add task cancellation:

```http
POST /api/circles/{circleId}/tasks/{taskId}/cancel
```

That endpoint should mark an open task as `CANCELLED` without deleting it.
