# Step 15 - Audit Baseline

## Goal

Add a reusable audit layer for sensitive MVP backend actions.

The goal is traceability, not analytics. Audit logs must help answer who
performed a sensitive action, against which entity, and with minimal operational
context.

## Technical Decision

Audit is implemented under `shared.audit` because several modules need it:

- `circles`
- `members`
- `companionrequests`
- `privacy`

The database table already existed from the initial schema:

- `audit_logs`

No migration is needed for this step.

## Main Components

- `AuditLog`: JPA entity mapped to `audit_logs`.
- `AuditAction`: stable action names.
- `AuditEntityType`: stable entity type names.
- `AuditLogRepository`: read/write access for audit entries.
- `AuditLogService`: single write entry point for application services.

## Audited Actions

Current baseline:

- `CARE_CIRCLE_CREATED`
- `CARE_CIRCLE_UPDATED`
- `CIRCLE_MEMBER_ADDED`
- `CIRCLE_MEMBER_ROLE_UPDATED`
- `CIRCLE_MEMBER_REMOVED`
- `COMPANION_REQUEST_CREATED`
- `COMPANION_REQUEST_CANCELLED`
- `CONSENT_ACCEPTED`
- `CONSENT_REVOKED`

## Metadata Rules

Audit metadata must stay minimal.

Allowed examples:

- ids
- roles
- status names
- changed field names
- booleans like `hasNotes`
- dates needed for operational context

Forbidden examples:

- private notes
- medication names or doses
- check-in free text
- companion request reason text
- full request bodies
- tokens or secrets
- email addresses when an internal user id is enough

## Transaction Rule

Audit entries are written from the same application transaction as the action.

If the business operation rolls back, its audit entry rolls back too. This keeps
the audit trail aligned with committed state.

## Verification

Run:

```powershell
.\mvnw.cmd clean test
```

Expected result:

- all tests pass
- JPA validates `audit_logs`
- audit assertions pass in circles, members, companion requests, and privacy
