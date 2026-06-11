package com.carecircle.api.shared.exception;

/**
 * Exception used when a request conflicts with the current application state.
 */
public class ResourceConflictException extends RuntimeException {

    /**
     * Creates a conflict exception with a stable API message.
     *
     * @param message message returned by the API error handler.
     */
    public ResourceConflictException(String message) {
        super(message);
    }
}
