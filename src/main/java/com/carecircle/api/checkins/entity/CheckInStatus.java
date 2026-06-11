package com.carecircle.api.checkins.entity;

/**
 * Non-clinical family signal captured by a care circle check-in.
 */
public enum CheckInStatus {
    /**
     * The family member reports that everything seems fine.
     */
    OK,

    /**
     * The family member reports that follow-up or family attention may be needed.
     */
    NEEDS_ATTENTION,

    /**
     * The family member could not reach or observe the elder.
     */
    NO_RESPONSE
}
