# CareCircle API

Backend API for the CareCircle MVP.

CareCircle helps families coordinate daily care for an older relative. It is not
a medical product: it does not diagnose, recommend treatments, modify
medication, replace health or social care professionals, or assign volunteers
directly.

## Stack

- Java 21
- Spring Boot 4
- Maven
- PostgreSQL
- Flyway
- Spring Web MVC
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server / JWT
- Supabase Auth
- Jakarta Validation
- Lombok
- MapStruct
- Springdoc OpenAPI / Swagger
- Actuator

## Architecture

The backend is a modular monolith.

Current modules:

- `auth`
- `users`
- `circles`
- `members`
- `elderprofiles`
- `tasks`
- `appointments`
- `checkins`
- `medications`
- `summaries`
- `companionrequests`
- `privacy`
- `shared`
- `common`

## Authentication

Supabase Auth is the identity provider.

Expected flow:

1. The client signs up or signs in with Supabase.
2. Supabase returns a JWT.
3. The client sends the token to this API:

```http
Authorization: Bearer <supabase_jwt>
```

4. The API validates the JWT.
5. The API synchronizes the internal `users` row using the JWT `sub` claim.

This backend does not implement email/password login and does not store
passwords.

## Roles

CareCircle has two role scopes.

### Global User Roles

Stored in `users.global_role`:

- `USER`
- `ADMIN`
- `PARTNER_USER`

These are application-level roles. Family permissions do not live here.

### Circle Roles

Stored in `circle_members.role`:

- `MAIN_CAREGIVER`
- `COLLABORATOR`
- `OBSERVER`

These roles are scoped to a care circle.

## Local Setup

### 1. Requirements

- Java 21
- Docker Desktop
- Maven wrapper from this repo

### 2. Environment

Create a local `.env` file in the repository root:

```properties
DB_HOST=localhost
DB_PORT=5433
DB_NAME=carecircle
DB_USERNAME=carecircle
DB_PASSWORD=carecircle

SUPABASE_PROJECT_URL=https://your-project.supabase.co
SUPABASE_ISSUER=https://your-project.supabase.co/auth/v1
SUPABASE_JWKS_URI=https://your-project.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_AUDIENCE=authenticated
```

Do not commit real secrets.

### 3. Start PostgreSQL

```powershell
docker compose up -d
```

### 4. Run Tests

```powershell
.\mvnw.cmd clean test
```

### 5. Start API

```powershell
.\mvnw.cmd spring-boot:run
```

## Local URLs

- Health: [http://localhost:8080/api/actuator/health](http://localhost:8080/api/actuator/health)
- Swagger UI: [http://localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs)

## Main Endpoints

All endpoints below require a bearer token unless noted otherwise.

### Auth

- `GET /api/auth/me`

### Care Circles

- `POST /api/circles`
- `GET /api/circles`
- `GET /api/circles/{circleId}`
- `PATCH /api/circles/{circleId}`

### Elder Profile

- `PATCH /api/circles/{circleId}/elder-profile`

### Members

- `GET /api/circles/{circleId}/members`
- `POST /api/circles/{circleId}/members`
- `PATCH /api/circles/{circleId}/members/{memberId}`
- `DELETE /api/circles/{circleId}/members/{memberId}`

### Tasks

- `POST /api/circles/{circleId}/tasks`
- `GET /api/circles/{circleId}/tasks`
- `PATCH /api/circles/{circleId}/tasks/{taskId}`
- `POST /api/circles/{circleId}/tasks/{taskId}/complete`
- `POST /api/circles/{circleId}/tasks/{taskId}/cancel`

### Appointments

- `POST /api/circles/{circleId}/appointments`
- `GET /api/circles/{circleId}/appointments`
- `PATCH /api/circles/{circleId}/appointments/{appointmentId}`
- `POST /api/circles/{circleId}/appointments/{appointmentId}/cancel`

### Check-ins

- `POST /api/circles/{circleId}/checkins`
- `GET /api/circles/{circleId}/checkins`

### Medications

- `POST /api/circles/{circleId}/medication-reminders`
- `GET /api/circles/{circleId}/medication-reminders`
- `PATCH /api/circles/{circleId}/medication-reminders/{reminderId}`
- `POST /api/circles/{circleId}/medication-reminders/{reminderId}/archive`
- `POST /api/circles/{circleId}/medication-intake-logs`
- `GET /api/circles/{circleId}/medication-intake-logs`

### Weekly Summaries

- `GET /api/circles/{circleId}/summaries/weekly?weekStart=YYYY-MM-DD`

### Companion Requests

- `POST /api/circles/{circleId}/companion-requests`
- `GET /api/circles/{circleId}/companion-requests`
- `POST /api/circles/{circleId}/companion-requests/{requestId}/cancel`

Creating a companion request requires accepted active consents for:

- `COMPANION_CONSENT`
- `COMPANION_DATA_SHARING`

### Privacy

- `GET /api/privacy/legal-documents`
- `GET /api/privacy/me`
- `POST /api/privacy/consents`
- `POST /api/privacy/consents/{consentRecordId}/revoke`

## Error Contract

Handled API errors return:

```json
{
  "timestamp": "2026-06-15T13:00:00+02:00",
  "status": 401,
  "error": "Unauthorized",
  "code": "AUTHENTICATION_REQUIRED",
  "message": "Authentication is required or token is invalid.",
  "path": "/api/auth/me",
  "traceId": "uuid",
  "fieldErrors": []
}
```

Stable error codes:

- `AUTHENTICATION_REQUIRED`
- `FORBIDDEN`
- `RESOURCE_NOT_FOUND`
- `VALIDATION_ERROR`
- `MALFORMED_REQUEST`
- `RESOURCE_CONFLICT`
- `INTERNAL_ERROR`

Swagger documents common error responses globally for controller operations.

## Audit

Audit logs are stored in `audit_logs`.

Current audited actions include:

- care circle creation/update
- member add/role update/removal
- companion request creation/cancellation
- consent acceptance/revocation

Audit metadata must stay minimal and must not include private notes, medication
details, check-in text, request bodies, tokens, or secrets.

## Product Boundaries

The MVP backend intentionally does not include:

- medical diagnosis
- treatment recommendation
- medication changes
- direct volunteer assignment
- partner organization panel
- AI features
- microservices
- frontend code

## Useful Documentation

- `docs/backend/local-environment.md`
- `docs/backend/step-10-api-error-contract.md`
- `docs/backend/step-14-privacy-consents.md`
- `docs/backend/step-15-audit-baseline.md`
- `docs/backend/step-16-permissions-review.md`
