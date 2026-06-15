# Step 16 - Permissions Review

## Goal

Review and document the current MVP authorization model before adding more
features.

This step does not introduce a new permission system. It documents the rules
already enforced by application services.

## Role Types

CareCircle has two role scopes.

### Global User Roles

Stored on `users.global_role`.

- `USER`: regular CareCircle user.
- `ADMIN`: future application administrator role.
- `PARTNER_USER`: future verified partner organization user role.

These are application-level roles. They do not define family permissions inside
a care circle.

### Circle Roles

Stored on `circle_members.role`.

- `MAIN_CAREGIVER`: can manage circle-level data and members.
- `COLLABORATOR`: can collaborate on day-to-day care operations.
- `OBSERVER`: read-only role for shared circle information.

Circle roles are scoped to one care circle.

## Current Access Matrix

All business endpoints require a valid Supabase bearer token.

| Module | Read | Create / Update / Action |
| --- | --- | --- |
| Auth | Authenticated user can call `/auth/me` | Not applicable |
| Care circles | Active members can list/get visible circles | Create by authenticated user; update only `MAIN_CAREGIVER` |
| Elder profile | Active members can view through circle response | Update only `MAIN_CAREGIVER` |
| Members | Active members can list | Add/update/remove only `MAIN_CAREGIVER` |
| Tasks | Active members can list | `MAIN_CAREGIVER` and `COLLABORATOR` can create/update/complete/cancel |
| Appointments | Active members can list | `MAIN_CAREGIVER` and `COLLABORATOR` can create/update/cancel |
| Check-ins | Active members can list | `MAIN_CAREGIVER` and `COLLABORATOR` can create |
| Medication reminders | Active members can list | `MAIN_CAREGIVER` and `COLLABORATOR` can create/update/archive |
| Medication intake logs | Active members can list | `MAIN_CAREGIVER` and `COLLABORATOR` can create |
| Weekly summaries | Active members can read | Computed read-only endpoint |
| Companion requests | Active members can list | `MAIN_CAREGIVER` and `COLLABORATOR` can create/cancel after companion consent |
| Privacy | Authenticated user can view own privacy status | Authenticated user can accept/revoke own allowed consent records |

## Visibility Rules

Missing membership is returned as `404`, not `403`.

This avoids revealing whether a private family resource exists to users outside
the care circle.

## Current Intentional Gaps

- `ADMIN` and `PARTNER_USER` are modeled but not yet used for special access.
- There is no main caregiver transfer workflow.
- There is no account deletion/deactivation workflow.
- There is no partner organization panel or partner request workflow.
- There are no per-circle feature flags or granular field-level permissions.

These are intentional MVP boundaries unless product scope changes.

## Verification

Run:

```powershell
.\mvnw.cmd clean test
```

Expected result:

- all tests pass
- permission tests continue returning `401`, `403`, `404`, and `409` consistently
