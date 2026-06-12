# Step 14 - Privacy Consents

## Goal

Add the MVP privacy backend needed to track legal document acceptance and
explicit companion-related consent.

This step does not add an admin panel, legal CMS, account deletion, or partner
workflow automation.

## Product Scope

CareCircle must keep trust and minimization visible in the backend model:

- users accept versioned terms, privacy policy, and non-medical disclaimer
- companion-related consents are tracked explicitly
- acceptance history is kept through append-only consent records
- optional companion consents can be revoked without deleting history
- audit logs store operational metadata only, not private data snapshots

## Data Model

Existing tables from `V1__init_schema.sql` are used:

- `legal_documents`: versioned metadata for active legal documents
- `consent_records`: user acceptance records for specific document versions
- `audit_logs`: basic audit trail for privacy-sensitive actions

`V8__seed_initial_legal_documents.sql` seeds the initial MVP documents:

- `TERMS_OF_SERVICE`
- `PRIVACY_POLICY`
- `MEDICAL_DISCLAIMER`
- `COMPANION_CONSENT`
- `COMPANION_DATA_SHARING`

## Endpoints

All endpoints require a valid Supabase bearer token.

```http
GET /api/privacy/legal-documents
```

Returns active legal document versions.

```http
GET /api/privacy/me
```

Returns the authenticated user's acceptance status for active documents.

```http
POST /api/privacy/consents
```

Accepts the active version of a document type.

Example:

```json
{
  "documentType": "PRIVACY_POLICY"
}
```

```http
POST /api/privacy/consents/{consentRecordId}/revoke
```

Revokes optional companion-related consents.

Required account legal documents cannot be revoked through this endpoint
because account deactivation or deletion is a separate workflow.

## Business Rules

- Accepting the same active document twice is idempotent.
- Accepting a newer active version revokes older active consent records for the
  same type.
- Accepting `TERMS_OF_SERVICE`, `PRIVACY_POLICY`, or `MEDICAL_DISCLAIMER`
  updates the direct timestamp columns on `users`.
- Optional companion consents are stored only in `consent_records`.
- Revoking someone else's consent returns `404`.
- Revoking a required legal document returns `409`.
- Missing or invalid bearer token returns the standard API error contract.

## Audit Rules

Audit records are written for:

- `CONSENT_ACCEPTED`
- `CONSENT_REVOKED`

Metadata is intentionally limited to:

- document type
- version

Do not store personal notes, family details, health information, or request
body snapshots in `audit_logs.metadata`.

## Verification

Run:

```powershell
.\mvnw.cmd clean test
```

Expected result:

- all tests pass
- Flyway validates 8 migrations
- JPA validates the schema
- Swagger exposes the `Privacy` tag
