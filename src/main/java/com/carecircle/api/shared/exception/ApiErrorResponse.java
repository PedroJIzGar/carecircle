package com.carecircle.api.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard JSON error response for API failures handled by CareCircle.
 *
 * @param timestamp time when the error response was produced.
 * @param status HTTP status code.
 * @param error short HTTP error name.
 * @param code stable machine-readable error code.
 * @param message human-readable technical message.
 * @param path request path.
 * @param traceId per-error identifier useful when correlating logs and client reports.
 * @param fieldErrors optional field-level validation details.
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        ApiErrorCode code,
        String message,
        String path,
        String traceId,
        List<ApiFieldError> fieldErrors
) {
}
