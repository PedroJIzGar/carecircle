# Step 7C - List current user's care circles

## Objective

Create the first read endpoint for care circles:

`GET /api/circles`

The endpoint returns only care circles where the authenticated user has an active membership.

## Why membership-based authorization

CareCircle roles are circle-scoped. A user may create a circle, but future users can also be collaborators or observers.

For that reason, the list endpoint does not filter by `care_circles.created_by_user_id`. It filters by:

- `circle_members.user_id`
- `circle_members.status = ACTIVE`

This prepares the backend for future permission checks without overengineering a full authorization framework yet.

## Response shape

The endpoint returns a list of `CareCircleResponse` items, the same aggregate shape used by `POST /api/circles`:

- care circle fields,
- nested elder profile,
- nested current membership.

## Files changed

- `circles/controller/CareCircleController.java`
- `circles/service/CareCircleService.java`
- `members/repository/CircleMemberRepository.java`
- `elderprofiles/repository/ElderProfileRepository.java`
- `circles/CareCircleControllerTests.java`

## Manual curl

```powershell
curl.exe -X GET "http://localhost:8080/api/circles" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

## Expected result

If the user has circles:

```json
[
  {
    "id": "care-circle-id",
    "name": "Garcia family",
    "description": "Daily care coordination",
    "status": "ACTIVE",
    "createdByUserId": "internal-user-id",
    "createdAt": "2026-06-11T10:00:00Z",
    "updatedAt": "2026-06-11T10:00:00Z",
    "elderProfile": {
      "id": "elder-profile-id",
      "fullName": "Maria Garcia",
      "preferredName": "Maria",
      "birthDate": "1945-03-12",
      "notes": "Prefers morning calls"
    },
    "currentMembership": {
      "id": "membership-id",
      "userId": "internal-user-id",
      "role": "MAIN_CAREGIVER",
      "status": "ACTIVE",
      "joinedAt": "2026-06-11T10:00:00Z"
    }
  }
]
```

If the user has no active memberships:

```json
[]
```

## Validation

Run:

```powershell
./mvnw clean test
```

Expected checks:

- authenticated users can list their active circle memberships,
- removed memberships are not returned,
- other users' circles are not returned,
- unauthenticated requests return `401`.

## Next step

The next backend step should be:

`GET /api/circles/{circleId}`

That endpoint should reuse the same membership-based authorization rule before returning one circle.
