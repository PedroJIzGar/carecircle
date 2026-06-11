package com.carecircle.api.shared.exception;

import java.time.OffsetDateTime;

/**
 * Standard JSON error response for API failures handled by CareCircle.
 *
 * @param timestamp time when the error response was produced.
 * @param status HTTP status code.
 * @param error short HTTP error name.
 * @param message human-readable technical message.
 * @param path request path.
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
