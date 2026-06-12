package com.carecircle.api.shared.audit.entity;

/**
 * Stable machine-readable audited entity types.
 */
public enum AuditEntityType {
    /**
     * Care circle aggregate.
     */
    CARE_CIRCLE,

    /**
     * Care circle membership.
     */
    CIRCLE_MEMBER,

    /**
     * Family companion request.
     */
    COMPANION_REQUEST,

    /**
     * User consent record.
     */
    CONSENT_RECORD
}
