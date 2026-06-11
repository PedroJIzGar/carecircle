# Step 8D - Remove Care Circle Member

## Goal

Add the first member removal endpoint:

```http
DELETE /api/circles/{circleId}/members/{memberId}
```

This endpoint lets the active `MAIN_CAREGIVER` remove a regular member from a care circle.

## Technical Decision

Membership removal is implemented as a soft delete:

- `circle_members.status` changes from `ACTIVE` to `REMOVED`.
- `circle_members.removed_at` is set.
- The row stays in the database.

This keeps enough traceability for future audit, legal consent, and family coordination history without exposing removed members in active reads.

## Authorization Rules

- The request requires a valid Supabase Bearer token.
- The authenticated user must be an active member of the circle.
- The authenticated user must have role `MAIN_CAREGIVER`.
- The target membership must belong to the requested circle and be `ACTIVE`.
- A `MAIN_CAREGIVER` membership cannot be removed through this endpoint.

Main caregiver removal or transfer needs a dedicated flow because it can leave a care circle without an owner.

## Files Changed

- `src/main/java/com/carecircle/api/members/controller/CircleMemberController.java`
- `src/main/java/com/carecircle/api/members/service/CircleMemberService.java`
- `src/test/java/com/carecircle/api/members/CircleMemberControllerTests.java`

## Manual Test

Use an access token for the main caregiver:

```powershell
curl.exe -X DELETE "http://localhost:8080/api/circles/TU_CIRCLE_ID/members/TU_MEMBER_ID" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

Expected result:

```http
204 No Content
```

Then call:

```powershell
curl.exe -X GET "http://localhost:8080/api/circles/TU_CIRCLE_ID/members" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

The removed member should no longer appear in the active member list.

## Expected Errors

- `401 Unauthorized`: missing, expired, or invalid Bearer token.
- `403 Forbidden`: authenticated user is a member but not `MAIN_CAREGIVER`.
- `404 Not Found`: requester is outside the circle, target membership does not exist, or target membership is not active.
- `409 Conflict`: target membership is `MAIN_CAREGIVER`.

## Automated Test

```powershell
.\mvnw.cmd clean test
```

The test suite covers:

- Successful soft removal by `MAIN_CAREGIVER`.
- Rejection when requester is a collaborator.
- Hidden circle/member existence for outside users.
- Rejection when target belongs to another circle.
- Rejection when target is the main caregiver.
- Authentication requirement.

## Next Step

After member management is complete, the recommended next module is `tasks` because it is the core coordination workflow for the MVP.
