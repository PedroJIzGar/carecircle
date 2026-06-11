package com.carecircle.api.shared.exception;

/**
 * Exception used when the authenticated user can see a resource but cannot
 * perform the requested operation on it.
 */
public class ForbiddenOperationException extends RuntimeException {

    /**
     * Creates a forbidden exception with a stable API message.
     *
     * @param message message returned by the API error handler.
     */
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
