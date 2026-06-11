# Step 8A - List care circle members

## Objective

Create the first members endpoint:

`GET /api/circles/{circleId}/members`

The endpoint returns active members in a care circle visible to the authenticated user.

## Authorization rule

The requester must have an active membership in the circle.

This endpoint does not require `MAIN_CAREGIVER` because reading the member list is part of normal circle coordination.

If the requester is not an active member, the API returns `404 Not Found` to avoid revealing private circle existence.

## Response fields

Each member response includes:

- membership id,
- internal user id,
- full name,
- email,
- avatar URL,
- circle role,
- membership status,
- joined timestamp,
- membership creation timestamp.

The response intentionally excludes:

- Supabase user id,
- phone,
- legal/consent timestamps,
- account administration fields.

## Manual curl

```powershell
curl.exe -X GET "http://localhost:8080/api/circles/TU_CIRCLE_ID/members" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

## Expected results

- `200 OK` with active members when the requester is an active member.
- `401 Unauthorized` when the Bearer token is missing or invalid.
- `404 Not Found` when the circle does not exist or the requester is not an active member.

## Validation

Run:

```powershell
./mvnw clean test
```

Expected checks:

- main caregiver can list active members,
- collaborator can list active members,
- removed members are not returned,
- outside users receive `404`,
- removed current membership receives `404`,
- unauthenticated requests receive `401`.

## Next step

The next backend step should be inviting or adding members:

`POST /api/circles/{circleId}/members`

That endpoint should be restricted to `MAIN_CAREGIVER` and should avoid creating users without a Supabase identity.
