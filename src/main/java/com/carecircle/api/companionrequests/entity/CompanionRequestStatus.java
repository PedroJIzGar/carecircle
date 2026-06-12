package com.carecircle.api.companionrequests.entity;

/**
 * Lifecycle state of a family companion request.
 */
public enum CompanionRequestStatus {
    /**
     * Request is active and waiting for future referral handling.
     */
    REQUESTED,

    /**
     * Request was cancelled by an authorized care circle member.
     */
    CANCELLED
}
