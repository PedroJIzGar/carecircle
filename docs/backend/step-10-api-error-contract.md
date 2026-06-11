# Step 10 - API Error Contract

## Goal

Standardize backend error responses before adding more MVP modules.

This keeps future frontend integration predictable and avoids parsing human
messages for application logic.

## Error Shape

All handled API errors should return:

```json
{
  "timestamp": "2026-06-11T18:00:00+02:00",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "title: must not be blank",
  "path": "/api/circles/{circleId}/tasks",
  "traceId": "uuid",
  "fieldErrors": [
    {
      "field": "title",
      "message": "must not be blank"
    }
  ]
}
```

Existing fields remain:

- `timestamp`
- `status`
- `error`
- `message`
- `path`

New fields:

- `code`: stable machine-readable value for clients.
- `traceId`: per-error id useful when reporting/debugging incidents.
- `fieldErrors`: field-level validation details.

## Error Codes

Initial codes:

- `AUTHENTICATION_REQUIRED`
- `FORBIDDEN`
- `RESOURCE_NOT_FOUND`
- `VALIDATION_ERROR`
- `MALFORMED_REQUEST`
- `RESOURCE_CONFLICT`
- `INTERNAL_ERROR`

## Security Errors

Spring Security errors happen before controller execution, so they do not pass
through `GlobalExceptionHandler`.

`SecurityErrorResponseHandler` standardizes:

- missing token
- invalid token
- security-level access denied errors

## Expected HTTP Mapping

- `400`: validation errors, malformed JSON, invalid path variables.
- `401`: missing or invalid authentication.
- `403`: authenticated but not allowed.
- `404`: missing or hidden resource.
- `409`: valid request conflicts with current state.
- `500`: unexpected server-side failure with a generic message.

## Files Changed

- `src/main/java/com/carecircle/api/shared/exception/ApiErrorResponse.java`
- `src/main/java/com/carecircle/api/shared/exception/ApiFieldError.java`
- `src/main/java/com/carecircle/api/shared/exception/ApiErrorCode.java`
- `src/main/java/com/carecircle/api/shared/exception/GlobalExceptionHandler.java`
- `src/main/java/com/carecircle/api/shared/exception/SecurityErrorResponseHandler.java`
- `src/main/java/com/carecircle/api/shared/config/SecurityConfig.java`
- `src/test/java/com/carecircle/api/shared/ApiErrorHandlingTests.java`

## Manual Checks

Missing token:

```powershell
curl.exe -X GET "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks"
```

Malformed JSON:

```powershell
curl.exe -X POST "http://localhost:8080/api/circles/TU_CIRCLE_ID/tasks" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d "{"
```

Invalid UUID:

```powershell
curl.exe -X GET "http://localhost:8080/api/circles/not-a-uuid/tasks" `
  -H "Authorization: Bearer TU_SUPABASE_ACCESS_TOKEN"
```

## Automated Test

```powershell
.\mvnw.cmd clean test
```

The tests cover:

- validation field errors
- malformed JSON
- invalid path variables
- unknown routes
- missing Bearer token
