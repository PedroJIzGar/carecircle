# Step 7E - Update care circle basics

## Objective

Create the first write endpoint after circle creation:

`PATCH /api/circles/{circleId}`

The endpoint updates care circle basics:

- `name`
- `description`

It does not update the elder profile. Elder profile data belongs to a separate domain concept and should have its own endpoint.

## Authorization rule

The endpoint requires:

- a valid Supabase Bearer token,
- an active membership in the requested circle,
- role `MAIN_CAREGIVER`.

Read endpoints allow any active member. This write endpoint is stricter because it changes shared circle configuration.

## Request example

```json
{
  "name": "Garcia family",
  "description": "Daily care coordination"
}
```

Partial updates are allowed:

```json
{
  "name": "Garcia family"
}
```

Blank `description` clears the description:

```json
{
  "description": " "
}
```

Blank `name` is rejected.

## Expected results

- `200 OK` when the authenticated user is the `MAIN_CAREGIVER`.
- `400 Bad Request` when no updatable fields are provided or the provided name is blank.
- `401 Unauthorized` when the Bearer token is missing or invalid.
- `403 Forbidden` when the user is an active member but not `MAIN_CAREGIVER`.
- `404 Not Found` when the circle does not exist or the user is not an active member.

## Manual curl

```powershell
curl.exe -X PATCH "http://localhost:8080/api/circles/TU_CIRCLE_ID" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "name": "Garcia family",
    "description": "Daily care coordination"
  }'
```

## Validation

Run:

```powershell
./mvnw clean test
```

Expected checks:

- main caregivers can update circle basics,
- collaborators receive `403`,
- users without active membership receive `404`,
- invalid patch bodies receive `400`,
- unauthenticated requests receive `401`.

## Next step

The next backend step should be the elder profile update endpoint:

`PATCH /api/circles/{circleId}/elder-profile`

That should also require `MAIN_CAREGIVER` at first.
