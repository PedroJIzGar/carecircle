# Step 8B - Add existing care circle member

## Objective

Create the first member write endpoint:

`POST /api/circles/{circleId}/members`

This endpoint adds an existing internal CareCircle user to a care circle.

## Important product decision

This is not a full invitation flow.

The target user must already exist in the `users` table. That means the user has already authenticated through Supabase and has been synchronized by `GET /api/auth/me` or another authenticated backend call.

The endpoint does not:

- create Supabase users,
- create placeholder users,
- send invitation emails,
- store invitation tokens.

Those belong to a future invitation workflow.

## Authorization rule

The requester must be:

- an active member of the circle,
- role `MAIN_CAREGIVER`.

If the requester is outside the circle, the API returns `404 Not Found`.

If the requester is an active member but not `MAIN_CAREGIVER`, the API returns `403 Forbidden`.

## Allowed roles

This endpoint allows adding:

- `COLLABORATOR`
- `OBSERVER`

It does not allow adding another `MAIN_CAREGIVER`. Transferring or sharing the main caregiver role should be designed as a separate feature.

## Request example

```json
{
  "email": "relative@example.com",
  "role": "COLLABORATOR"
}
```

## Expected results

- `201 Created` when the member is added.
- `400 Bad Request` when email or role is invalid.
- `401 Unauthorized` when the Bearer token is missing or invalid.
- `403 Forbidden` when the requester is active but not `MAIN_CAREGIVER`.
- `404 Not Found` when the circle is not visible or the target user does not exist.
- `409 Conflict` when the target user is already associated with the circle.

## Manual curl

```powershell
curl.exe -X POST "http://localhost:8080/api/circles/TU_CIRCLE_ID/members" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "email": "relative@example.com",
    "role": "COLLABORATOR"
  }'
```

## Validation

Run:

```powershell
./mvnw clean test
```

Expected checks:

- main caregiver can add an existing user,
- collaborator cannot add members,
- outside users receive `404`,
- missing target user receives `404`,
- duplicate membership receives `409`,
- invalid role or email receives `400`,
- unauthenticated requests receive `401`.

## Next step

The next member feature should be updating member roles:

`PATCH /api/circles/{circleId}/members/{memberId}`

That endpoint should be restricted to `MAIN_CAREGIVER` and should protect the last/main caregiver rule.
