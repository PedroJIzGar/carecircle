package com.carecircle.api.appointments.entity;

import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Non-clinical appointment used by family members to coordinate care logistics.
 *
 * <p>Appointments represent family organization only. They must not be used to
 * diagnose, recommend treatments, or modify medication.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "appointments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {

    /**
     * Internal appointment identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle where the appointment belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false)
    private CareCircle careCircle;

    /**
     * Short appointment title shown in lists.
     */
    @Column(name = "title", nullable = false, length = 160)
    private String title;

    /**
     * Optional appointment location or channel.
     */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * Optional family coordination notes.
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Current appointment lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    /**
     * Appointment start date and time.
     */
    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    /**
     * Optional appointment end date and time.
     */
    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    /**
     * User that created the appointment.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    /**
     * Cancellation timestamp, present only for cancelled appointments.
     */
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    /**
     * User that cancelled the appointment, present only for cancelled appointments.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledByUser;

    /**
     * Entity creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Creates a scheduled appointment in a care circle.
     *
     * @param careCircle circle that owns the appointment.
     * @param title short family-visible appointment title.
     * @param startsAt appointment start date and time.
     * @param createdByUser user creating the appointment.
     */
    public Appointment(CareCircle careCircle, String title, OffsetDateTime startsAt, User createdByUser) {
        this.careCircle = careCircle;
        this.title = title;
        this.startsAt = startsAt;
        this.createdByUser = createdByUser;
        this.status = AppointmentStatus.SCHEDULED;
    }

    /**
     * Ensures timestamps are present for new appointments created through JPA.
     */
    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Updates the modification timestamp before every JPA update.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
