package com.carecircle.api.auth.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Signals that a validated JWT does not contain the identity claims required by CareCircle.
 */
public class InvalidAuthenticationClaimsException extends AuthenticationException {

    /**
     * Creates an invalid authentication claims exception.
     *
     * @param message technical reason for the rejected claims.
     */
    public InvalidAuthenticationClaimsException(String message) {
        super(message);
    }
}
