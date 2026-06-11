# Step 9B - List Care Circle Tasks

## Goal

Add the task list endpoint:

```http
GET /api/circles/{circleId}/tasks
```

This returns tasks visible to an active member of the requested care circle.

## Technical Decision

Task visibility is based on active circle membership:

- `MAIN_CAREGIVER` can list tasks.
- `COLLABORATOR` can list tasks.
- `OBSERVER` can list tasks.
- Removed members and outside users cannot list tasks.

This matches the MVP goal: observers may follow the care coordination state without changing it.

## Ordering

The initial list order is intentionally simple:

1. `OPEN`
2. `COMPLETED`
3. `CANCELLED`
4. tasks with `dueAt` before tasks without `dueAt`
5. nearest due date first
6. newest creation date first as final tie-breaker

Filtering and pagination are left for a later step once the first task workflow is usable.

## Files Changed

- `src/main/java/com/carecircle/api/tasks/controller/CareTaskController.java`
- `src/main/java/com/carecircle/api/tasks/service/CareTaskService.java`
- `src/main/java/com/carecircle/api/tasks/repository/CareTaskRepository.java`
- `src/test/java/com/carecircle/api/tasks/CareTaskControllerTests.java`

## Manual Test

Use any active member token for the circle:

```powershell
curl.exe -X GET "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

Expected result:

```http
200 OK
```

The response is a JSON array of task DTOs.

## Expected Errors

- `401 Unauthorized`: missing, expired, or invalid Bearer token.
- `404 Not Found`: requester is outside the circle or no longer has an active membership.

## Automated Test

```powershell
.\mvnw.cmd clean test
```

The tests cover:

- listing tasks as an `OBSERVER`
- ordering by status and due date
- excluding tasks from other circles
- returning `404` for outside users
- returning `404` for removed members
- authentication requirement

## Next Step

Step 9C should add:

```http
PATCH /api/circles/{circleId}/tasks/{taskId}
```

That endpoint should update editable task fields such as title, description, due date, priority and assignment.
