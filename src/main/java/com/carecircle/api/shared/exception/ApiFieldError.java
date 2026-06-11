package com.carecircle.api.shared.exception;

/**
 * Field-level validation error returned by the API.
 *
 * @param field request field that failed validation.
 * @param message validation message for the field.
 */
public record ApiFieldError(
        String field,
        String message
) {
}
