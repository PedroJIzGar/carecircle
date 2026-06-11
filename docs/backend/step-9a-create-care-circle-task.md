# Step 9A - Create Care Circle Task

## Goal

Add the first task endpoint:

```http
POST /api/circles/{circleId}/tasks
```

This creates an `OPEN` non-clinical coordination task inside a care circle.

## Technical Decision

Tasks belong to a `CareCircle`, not directly to an elder profile. The circle is the collaboration boundary, and tasks are visible and permissioned through active circle membership.

The first task model stores:

- title
- optional description
- status
- priority
- optional due date/time
- optional assigned user
- creator
- completion fields reserved for the next step
- timestamps

## Product Boundary

CareCircle tasks are for family coordination only.

They must not be used to:

- diagnose
- recommend treatments
- change medication
- replace professional medical or social care advice

This is documented in the database comment and entity JavaDoc.

## Authorization Rules

- A valid Supabase Bearer token is required.
- The requester must be an active member of the care circle.
- `MAIN_CAREGIVER` can create tasks.
- `COLLABORATOR` can create tasks.
- `OBSERVER` cannot create tasks.
- If `assignedToUserId` is provided, that user must be an active member of the same circle.

## Files Added

- `src/main/resources/db/migration/V3__tasks_schema.sql`
- `src/main/java/com/carecircle/api/tasks/entity/CareTask.java`
- `src/main/java/com/carecircle/api/tasks/entity/TaskStatus.java`
- `src/main/java/com/carecircle/api/tasks/entity/TaskPriority.java`
- `src/main/java/com/carecircle/api/tasks/dto/CreateTaskRequest.java`
- `src/main/java/com/carecircle/api/tasks/dto/TaskResponse.java`
- `src/main/java/com/carecircle/api/tasks/repository/CareTaskRepository.java`
- `src/main/java/com/carecircle/api/tasks/mapper/CareTaskMapper.java`
- `src/main/java/com/carecircle/api/tasks/service/CareTaskService.java`
- `src/main/java/com/carecircle/api/tasks/controller/CareTaskController.java`
- `src/test/java/com/carecircle/api/tasks/CareTaskControllerTests.java`

## Manual Test

Use an access token from a `MAIN_CAREGIVER` or `COLLABORATOR` user:

```powershell
curl.exe -X POST "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "title": "Buy groceries",
    "description": "Pick up fruit and water",
    "priority": "HIGH",
    "dueAt": "2026-06-12T10:00:00+02:00",
    "assignedToUserId": "TU_USER_ID"
  }'
```

Expected result:

```http
201 Created
```

The response should contain:

- `status: "OPEN"`
- `priority: "HIGH"` or `NORMAL` when omitted
- `createdByUserId`
- optional assignment fields

## Expected Errors

- `401 Unauthorized`: missing, expired, or invalid Bearer token.
- `403 Forbidden`: requester is an `OBSERVER`.
- `404 Not Found`: requester is outside the circle, or assigned user is not an active member.
- `400 Bad Request`: invalid body, blank title, past `dueAt`, or unsupported enum value.

## Automated Test

```powershell
.\mvnw.cmd clean test
```

The tests cover:

- successful task creation by `MAIN_CAREGIVER`
- default priority
- creation by `COLLABORATOR`
- rejection for `OBSERVER`
- hidden resources for outside users
- assigned user membership validation
- request validation
- authentication requirement

## Next Step

Step 9B should add:

```http
GET /api/circles/{circleId}/tasks
```

That endpoint should list tasks visible to active circle members, probably ordered by open status and due date.
