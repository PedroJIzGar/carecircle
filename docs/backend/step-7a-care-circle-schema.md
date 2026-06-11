# Step 7A - Care circle base model

## Objective

Prepare the persistence model for the next backend feature: creating a care circle, creating the elder profile and assigning the authenticated user as `MAIN_CAREGIVER`.

This step does not expose new HTTP endpoints. It only adds the database schema, JPA entities and repositories needed by the next service layer.

## Domain decisions

- `users` remains the identity/account table synchronized from Supabase Auth.
- `care_circles` represents the family care space.
- `elder_profiles` stores basic non-clinical information about the elder person associated with the circle.
- `circle_members` connects users to care circles and stores family roles.
- Family roles are scoped to a circle and must not be stored in `users.global_role`.
- The database allows a user to belong to multiple circles. MVP service rules can restrict that later without forcing a restrictive schema now.

## Tables

### care_circles

Stores the coordination space.

Important fields:

- `name`
- `description`
- `status`
- `created_by_user_id`
- timestamps

### elder_profiles

Stores one basic elder profile per care circle.

Important fields:

- `care_circle_id`
- `full_name`
- `preferred_name`
- `birth_date`
- `notes`
- timestamps

The `notes` field is for general family notes only. It must not become a diagnosis, treatment or clinical recommendation record.

### circle_members

Stores membership and role inside a circle.

Supported roles:

- `MAIN_CAREGIVER`
- `COLLABORATOR`
- `OBSERVER`

Supported statuses:

- `ACTIVE`
- `INVITED`
- `REMOVED`

## How to validate

Run:

```powershell
./mvnw clean test
```

Expected result:

- Flyway applies `V2__care_circle_schema.sql`.
- Hibernate validates the JPA mappings against PostgreSQL.
- `CareCircleModelTests` persists a user, care circle, elder profile and main caregiver membership inside a transaction.

## Next step

The next step should create the first write endpoint:

`POST /api/circles`

That endpoint should:

- require a valid Supabase Bearer token,
- synchronize the current user if needed,
- create a care circle,
- create the elder profile,
- create a `MAIN_CAREGIVER` membership for the current user,
- return a response DTO.
