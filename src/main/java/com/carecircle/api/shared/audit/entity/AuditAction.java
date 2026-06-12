package com.carecircle.api.shared.audit.entity;

/**
 * Stable machine-readable action names stored in audit logs.
 */
public enum AuditAction {
    /**
     * A care circle was created.
     */
    CARE_CIRCLE_CREATED,

    /**
     * Care circle basic details were updated.
     */
    CARE_CIRCLE_UPDATED,

    /**
     * A user was added to a care circle.
     */
    CIRCLE_MEMBER_ADDED,

    /**
     * A care circle member role was updated.
     */
    CIRCLE_MEMBER_ROLE_UPDATED,

    /**
     * A care circle member was removed.
     */
    CIRCLE_MEMBER_REMOVED,

    /**
     * A companion request was created.
     */
    COMPANION_REQUEST_CREATED,

    /**
     * A companion request was cancelled.
     */
    COMPANION_REQUEST_CANCELLED,

    /**
     * A legal document or consent was accepted.
     */
    CONSENT_ACCEPTED,

    /**
     * An optional consent was revoked.
     */
    CONSENT_REVOKED
}
