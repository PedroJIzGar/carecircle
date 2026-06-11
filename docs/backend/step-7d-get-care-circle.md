# Step 7D - Get one care circle

## Objective

Create the detail endpoint:

`GET /api/circles/{circleId}`

The endpoint returns one care circle only when the authenticated user has an active membership in that circle.

## Authorization rule

The endpoint checks:

- `circle_members.care_circle_id = {circleId}`
- `circle_members.user_id = currentUser.id`
- `circle_members.status = ACTIVE`

If that membership does not exist, the API returns `404 Not Found`.

## Why 404 instead of 403

Returning `404` avoids leaking whether a care circle exists to users who are not members. For private family data, this is the safer default.

## Response shape

The endpoint returns the same `CareCircleResponse` used by:

- `POST /api/circles`
- `GET /api/circles`

This keeps the API predictable for future frontend screens.

## Manual curl

```powershell
curl.exe -X GET "http://localhost:8080/api/circles/TU_CIRCLE_ID" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

## Expected results

- `200 OK` when the user has an active membership.
- `401 Unauthorized` when the Bearer token is missing or invalid.
- `404 Not Found` when the circle does not exist or the user is not an active member.

## Validation

Run:

```powershell
./mvnw clean test
```

Expected checks:

- active members can read the circle detail,
- other users receive `404`,
- removed memberships receive `404`,
- unauthenticated requests receive `401`.

## Next step

The next backend step should be an update endpoint for the care circle basics:

`PATCH /api/circles/{circleId}`

That endpoint should allow only `MAIN_CAREGIVER` at first, because changing circle data is stronger than simply reading it.
