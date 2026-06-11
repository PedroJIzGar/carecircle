# Step 7B - Create care circle endpoint

## Objective

Create the first care circle feature:

`POST /api/circles`

The endpoint creates:

- one care circle,
- one basic elder profile,
- one membership for the authenticated user with role `MAIN_CAREGIVER`.

## Why this shape

The authenticated user must come from the Supabase Bearer token, not from the request body. This prevents a client from creating a circle on behalf of another user.

The operation is implemented in a single transaction. If one insert fails, all inserts are rolled back.

## Request example

```json
{
  "circle": {
    "name": "Garcia family",
    "description": "Daily care coordination"
  },
  "elderProfile": {
    "fullName": "Maria Garcia",
    "preferredName": "Maria",
    "birthDate": "1945-03-12",
    "notes": "Prefers morning calls"
  }
}
```

## Response example

```json
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
```

## Files changed

- `circles/controller/CareCircleController.java`
- `circles/dto/CreateCareCircleRequest.java`
- `circles/dto/CareCircleResponse.java`
- `circles/mapper/CareCircleMapper.java`
- `circles/service/CareCircleService.java`
- `users/service/UserService.java`
- `shared/exception/GlobalExceptionHandler.java`
- `circles/CareCircleControllerTests.java`

## Validation

Run:

```powershell
./mvnw clean test
```

Expected result:

- unauthorized requests return `401`,
- invalid request bodies return `400`,
- valid authenticated requests return `201`,
- the database contains the care circle, elder profile and `MAIN_CAREGIVER` membership.

## Manual curl

```powershell
curl.exe -X POST "http://localhost:8080/api/circles" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "circle": {
      "name": "Garcia family",
      "description": "Daily care coordination"
    },
    "elderProfile": {
      "fullName": "Maria Garcia",
      "preferredName": "Maria",
      "birthDate": "1945-03-12",
      "notes": "Prefers morning calls"
    }
  }'
```

## Next step

After this endpoint works, the next backend step should be reading the current user's circles:

`GET /api/circles`

That will introduce authorization checks based on `circle_members`.
