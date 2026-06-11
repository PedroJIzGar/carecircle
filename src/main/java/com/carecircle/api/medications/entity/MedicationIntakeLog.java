package com.carecircle.api.medications.entity;

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
 * Manual family log for a medication reminder.
 *
 * <p>Logs reflect what a family member recorded. CareCircle does not decide
 * whether medication should be taken, skipped, started, stopped, or changed.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "medication_intake_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationIntakeLog {

    /**
     * Internal intake log identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle where the log belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false)
    private CareCircle careCircle;

    /**
     * Reminder this log was recorded against.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_id", nullable = false)
    private MedicationReminder reminder;

    /**
     * Manual family-recorded intake status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MedicationIntakeStatus status;

    /**
     * Time the family log refers to.
     */
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    /**
     * Optional family coordination note.
     */
    @Column(name = "note", length = 1000)
    private String note;

    /**
     * User that recorded the intake log.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private User recordedByUser;

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
     * Creates a manual medication intake log.
     *
     * @param careCircle circle that owns the log.
     * @param reminder reminder the log belongs to.
     * @param status family-recorded intake status.
     * @param occurredAt time the log refers to.
     * @param recordedByUser user recording the log.
     */
    public MedicationIntakeLog(
            CareCircle careCircle,
            MedicationReminder reminder,
            MedicationIntakeStatus status,
            OffsetDateTime occurredAt,
            User recordedByUser
    ) {
        this.careCircle = careCircle;
        this.reminder = reminder;
        this.status = status;
        this.occurredAt = occurredAt;
        this.recordedByUser = recordedByUser;
    }

    /**
     * Ensures timestamps are present for new logs created through JPA.
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
