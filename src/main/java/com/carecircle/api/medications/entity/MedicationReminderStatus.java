package com.carecircle.api.medications.entity;

/**
 * Lifecycle state of a family medication reminder.
 */
public enum MedicationReminderStatus {
    /**
     * Reminder is visible and can receive intake logs.
     */
    ACTIVE,

    /**
     * Reminder has been archived and no longer accepts new intake logs.
     */
    ARCHIVED
}
