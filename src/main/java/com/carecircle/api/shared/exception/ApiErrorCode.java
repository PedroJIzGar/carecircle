package com.carecircle.api.shared.exception;

/**
 * Stable machine-readable error codes for API clients.
 */
public enum ApiErrorCode {
    /**
     * Request authentication is missing or invalid.
     */
    AUTHENTICATION_REQUIRED,

    /**
     * Authenticated user is not allowed to perform the operation.
     */
    FORBIDDEN,

    /**
     * Requested resource does not exist or is not visible to the user.
     */
    RESOURCE_NOT_FOUND,

    /**
     * Request body, path variable, query parameter, or field value is invalid.
     */
    VALIDATION_ERROR,

    /**
     * Request body could not be parsed.
     */
    MALFORMED_REQUEST,

    /**
     * Request conflicts with current application or database state.
     */
    RESOURCE_CONFLICT,

    /**
     * Unexpected server-side failure.
     */
    INTERNAL_ERROR
}
