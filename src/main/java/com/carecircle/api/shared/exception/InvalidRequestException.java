package com.carecircle.api.shared.exception;

/**
 * Signals that the request is syntactically valid but violates an expected API rule.
 */
public class InvalidRequestException extends RuntimeException {

    /**
     * Creates an invalid request exception.
     *
     * @param message technical validation message.
     */
    public InvalidRequestException(String message) {
        super(message);
    }
}
