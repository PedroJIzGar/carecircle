package com.carecircle.api.shared.exception;

/**
 * Exception used when a requested resource does not exist or is not visible to
 * the authenticated user.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates a not-found exception with a stable API message.
     *
     * @param message message returned by the API error handler.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
