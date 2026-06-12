package com.carecircle.api.companionrequests.entity;

/**
 * Verification lifecycle state of a partner organization.
 */
public enum PartnerOrganizationStatus {
    /**
     * Organization is verified and can be used in future referral workflows.
     */
    VERIFIED,

    /**
     * Organization is not currently available for referrals.
     */
    INACTIVE
}
