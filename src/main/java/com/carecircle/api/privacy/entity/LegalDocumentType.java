package com.carecircle.api.privacy.entity;

/**
 * Versioned legal document and consent categories tracked by CareCircle.
 */
public enum LegalDocumentType {
    /**
     * General application terms accepted by an authenticated user.
     */
    TERMS_OF_SERVICE,

    /**
     * Privacy policy explaining personal data processing.
     */
    PRIVACY_POLICY,

    /**
     * Explicit reminder that CareCircle is not a medical product.
     */
    MEDICAL_DISCLAIMER,

    /**
     * Consent required before requesting safe companionship support.
     */
    COMPANION_CONSENT,

    /**
     * Consent describing the limited data shared with verified partner organizations.
     */
    COMPANION_DATA_SHARING
}
