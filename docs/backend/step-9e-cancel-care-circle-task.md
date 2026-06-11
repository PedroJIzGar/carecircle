# Step 9E - Cancel Care Circle Task

## Goal

Add the task cancellation endpoint:

```http
POST /api/circles/{circleId}/tasks/{taskId}/cancel
```

This marks an open task as cancelled without deleting it.

## Technical Decision

Task cancellation is modeled as an explicit action endpoint.

The task row is kept because cancellation is part of the coordination history.
For now, cancellation writes:

- `status = CANCELLED`

The existing `updatedAt` timestamp records when the row changed. If we later
need user-level cancellation audit, we should use `audit_logs` rather than
adding action-specific columns for every task transition.

## Authorization Rules

- A valid Supabase Bearer token is required.
- The requester must be an active member of the care circle.
- `MAIN_CAREGIVER` can cancel tasks.
- `COLLABORATOR` can cancel tasks.
- `OBSERVER` cannot cancel tasks.
- The task must belong to the requested care circle.
- Only `OPEN` tasks can be cancelled.

## Files Changed

- `src/main/java/com/carecircle/api/tasks/controller/CareTaskController.java`
- `src/main/java/com/carecircle/api/tasks/service/CareTaskService.java`
- `src/test/java/com/carecircle/api/tasks/CareTaskControllerTests.java`

## Manual Test

Use a token from a `MAIN_CAREGIVER` or `COLLABORATOR`:

```powershell
curl.exe -X POST "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks/TU_TASK_ID/cancel" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

Expected result:

```http
200 OK
```

The response should contain:

- `status: "CANCELLED"`
- no completion fields

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

- successful cancellation by `MAIN_CAREGIVER`
- cancelled state persisted in database
- rejection for `OBSERVER`
- hidden resources for outside users
- hidden tasks from other circles
- conflict when task is completed
- conflict when task is already cancelled
- authentication requirement

## Next Step

The core task lifecycle is now usable:

- create
- list
- update
- complete
- cancel

The next backend feature should be appointments, because appointments are the
next non-medical coordination object in the MVP roadmap.
