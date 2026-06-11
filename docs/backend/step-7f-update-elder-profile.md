# Step 7F - Update elder profile

## Objective

Create the elder profile update endpoint:

`PATCH /api/circles/{circleId}/elder-profile`

The endpoint updates basic non-clinical elder profile fields:

- `fullName`
- `preferredName`
- `birthDate`
- `notes`

It does not store diagnosis, treatment decisions or medical recommendations.

## Authorization rule

The endpoint requires:

- a valid Supabase Bearer token,
- an active membership in the requested circle,
- role `MAIN_CAREGIVER`.

This matches the current rule for changing circle basics.

## Request example

```json
{
  "fullName": "Maria Garcia",
  "preferredName": "Maria",
  "birthDate": "1945-03-12",
  "notes": "Prefers morning calls"
}
```

Partial updates are allowed:

```json
{
  "preferredName": "Maria"
}
```

Blank optional text fields clear the value:

```json
{
  "preferredName": " ",
  "notes": " "
}
```

Blank `fullName` is rejected.

## Important limitation

The current simple PATCH DTO cannot distinguish between an omitted `birthDate` and an explicit JSON `null`. For now, birth date can be set or left unchanged, but not cleared through this endpoint.

If clearing nullable dates becomes necessary, introduce a nullable-field wrapper such as `JsonNullable` or a dedicated clear operation.

## Expected results

- `200 OK` when the authenticated user is the `MAIN_CAREGIVER`.
- `400 Bad Request` when no fields are provided, `fullName` is blank, or `birthDate` is in the future.
- `401 Unauthorized` when the Bearer token is missing or invalid.
- `403 Forbidden` when the user is an active member but not `MAIN_CAREGIVER`.
- `404 Not Found` when the circle does not exist or the user is not an active member.

## Manual curl

```powershell
curl.exe -X PATCH "http://localhost:8080/api/circles/TU_CIRCLE_ID/elder-profile" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{
    "fullName": "Maria Garcia",
    "preferredName": "Maria",
    "birthDate": "1945-03-12",
    "notes": "Prefers morning calls"
  }'
```

## Validation

Run:

```powershell
./mvnw clean test
```

Expected checks:

- main caregivers can update elder profile basics,
- collaborators receive `403`,
- users without active membership receive `404`,
- invalid patch bodies receive `400`,
- unauthenticated requests receive `401`.

## Next step

The next backend step should introduce member invitation/read endpoints, or start the first task module endpoint once circle membership permissions are stable.
