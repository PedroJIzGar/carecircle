package com.carecircle.api.appointments.entity;

/**
 * Lifecycle state of a care circle appointment.
 */
public enum AppointmentStatus {
    /**
     * Appointment is planned and still active.
     */
    SCHEDULED,

    /**
     * Appointment was cancelled by an authorized care circle member.
     */
    CANCELLED
}
