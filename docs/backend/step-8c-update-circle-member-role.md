# Step 8C - Update care circle member role

## Objective

Create the member role update endpoint:

`PATCH /api/circles/{circleId}/members/{memberId}`

This endpoint lets the `MAIN_CAREGIVER` change a regular member between:

- `COLLABORATOR`
- `OBSERVER`

## Authorization rule

The requester must be:

- an active member of the circle,
- role `MAIN_CAREGIVER`.

If the requester is outside the circle, the API returns `404 Not Found`.

If the requester is active but not `MAIN_CAREGIVER`, the API returns `403 Forbidden`.

## Main caregiver safety rule

This endpoint does not create, transfer or remove the `MAIN_CAREGIVER` role.

Changing the main caregiver is a higher-risk workflow because the circle must not be left without a responsible owner. That should be implemented as a dedicated feature later.

## Request example

```json
{
  "role": "OBSERVER"
}
```

## Expected results

- `200 OK` when the member role is updated.
- `400 Bad Request` when the requested role is invalid.
- `401 Unauthorized` when the Bearer token is missing or invalid.
- `403 Forbidden` when the requester is active but not `MAIN_CAREGIVER`.
- `404 Not Found` when the circle is not visible or the member does not belong to that circle.
- `409 Conflict` when trying to change a `MAIN_CAREGIVER` membership through this endpoint.

## Manual curl

```powershell
curl.exe -X PATCH "http://localhost:8080/api/circles/TU_CIRCLE_ID/members/TU_MEMBER_ID" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "role": "OBSERVER"
  }'
```

## Validation

Run:

```powershell
./mvnw clean test
```

Expected checks:

- main caregiver can update regular member roles,
- collaborator cannot update roles,
- outside users receive `404`,
- members from another circle receive `404`,
- main caregiver membership changes receive `409`,
- invalid roles receive `400`,
- unauthenticated requests receive `401`.

## Next step

The next member feature should be removing a member:

`DELETE /api/circles/{circleId}/members/{memberId}`

That endpoint should mark the membership as `REMOVED`, not physically delete it.
