# Step 9C - Update Care Circle Task

## Goal

Add the task update endpoint:

```http
PATCH /api/circles/{circleId}/tasks/{taskId}
```

This updates editable fields of an open non-clinical coordination task.

## Technical Decision

Only `OPEN` tasks can be edited through this endpoint.

Completed or cancelled tasks represent history and need dedicated workflows if
the product later allows reopening or correcting them.

## Editable Fields

The endpoint can update:

- `title`
- `description`
- `priority`
- `dueAt`
- `assignedToUserId`

Because this is a PATCH endpoint, absent fields mean "do not change". To reset
optional fields, the request uses explicit clear flags:

- `clearDescription`
- `clearDueAt`
- `clearAssignment`

This keeps the MVP simple without introducing JSON Merge Patch or additional
nullable-field libraries.

## Authorization Rules

- A valid Supabase Bearer token is required.
- The requester must be an active member of the care circle.
- `MAIN_CAREGIVER` can update tasks.
- `COLLABORATOR` can update tasks.
- `OBSERVER` cannot update tasks.
- The task must belong to the requested care circle.
- If `assignedToUserId` is provided, that user must be an active member of the same circle.

## Files Changed

- `src/main/java/com/carecircle/api/tasks/controller/CareTaskController.java`
- `src/main/java/com/carecircle/api/tasks/dto/UpdateTaskRequest.java`
- `src/main/java/com/carecircle/api/tasks/repository/CareTaskRepository.java`
- `src/main/java/com/carecircle/api/tasks/service/CareTaskService.java`
- `src/test/java/com/carecircle/api/tasks/CareTaskControllerTests.java`

## Manual Test

Use a token from a `MAIN_CAREGIVER` or `COLLABORATOR`:

```powershell
curl.exe -X PATCH "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks/TU_TASK_ID" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "title": "Buy groceries and water",
    "priority": "HIGH",
    "dueAt": "2026-06-12T10:00:00+02:00",
    "assignedToUserId": "TU_USER_ID"
  }'
```

Clear optional fields:

```powershell
curl.exe -X PATCH "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks/TU_TASK_ID" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "clearDescription": true,
    "clearDueAt": true,
    "clearAssignment": true
  }'
```

Expected result:

```http
200 OK
```

## Expected Errors

- `400 Bad Request`: blank title, empty update body, invalid enum, past `dueAt`, or conflicting set and clear fields.
- `401 Unauthorized`: missing, expired, or invalid Bearer token.
- `403 Forbidden`: requester is an `OBSERVER`.
- `404 Not Found`: requester is outside the circle, task belongs to another circle, or assigned user is not an active member.
- `409 Conflict`: task is not `OPEN`.

## Automated Test

```powershell
.\mvnw.cmd clean test
```

The tests cover:

- successful update by `COLLABORATOR`
- clearing optional fields
- rejection for `OBSERVER`
- hidden resources for outside users
- hidden tasks from other circles
- assigned user membership validation
- completed task conflict
- blank title validation
- conflicting set and clear validation
- authentication requirement

## Next Step

Step 9D should add task completion:

```http
POST /api/circles/{circleId}/tasks/{taskId}/complete
```

This should set `status = COMPLETED`, `completedAt`, and `completedByUser`.
