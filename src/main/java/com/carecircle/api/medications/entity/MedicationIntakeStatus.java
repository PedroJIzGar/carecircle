package com.carecircle.api.medications.entity;

/**
 * Manual family-recorded status for a medication reminder occurrence.
 */
public enum MedicationIntakeStatus {
    /**
     * A family member recorded that the medication was taken.
     */
    TAKEN,

    /**
     * A family member recorded that the medication was skipped.
     */
    SKIPPED
}
