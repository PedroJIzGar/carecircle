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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Family-entered medication reminder used for care coordination.
 *
 * <p>Medication reminders store information entered by family members. They
 * must not be treated as medical prescriptions, treatment recommendations, or
 * medication changes made by CareCircle.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "medication_reminders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationReminder {

    /**
     * Internal medication reminder identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle where the reminder belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false)
    private CareCircle careCircle;

    /**
     * Family-entered medication name.
     */
    @Column(name = "medication_name", nullable = false, length = 160)
    private String medicationName;

    /**
     * Optional family-entered dosage label.
     */
    @Column(name = "dosage_text", length = 160)
    private String dosageText;

    /**
     * Family-entered schedule label.
     */
    @Column(name = "schedule_text", nullable = false, length = 255)
    private String scheduleText;

    /**
     * Optional family coordination instructions.
     */
    @Column(name = "instructions", length = 1000)
    private String instructions;

    /**
     * Current reminder lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MedicationReminderStatus status = MedicationReminderStatus.ACTIVE;

    /**
     * Optional first date for the reminder.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Optional last date for the reminder.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * User that created the reminder.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    /**
     * Archive timestamp, present only for archived reminders.
     */
    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    /**
     * User that archived the reminder, present only for archived reminders.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by_user_id")
    private User archivedByUser;

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
     * Creates an active family medication reminder.
     *
     * @param careCircle circle that owns the reminder.
     * @param medicationName family-entered medication name.
     * @param scheduleText family-entered schedule label.
     * @param createdByUser user creating the reminder.
     */
    public MedicationReminder(
            CareCircle careCircle,
            String medicationName,
            String scheduleText,
            User createdByUser
    ) {
        this.careCircle = careCircle;
        this.medicationName = medicationName;
        this.scheduleText = scheduleText;
        this.createdByUser = createdByUser;
        this.status = MedicationReminderStatus.ACTIVE;
    }

    /**
     * Ensures timestamps are present for new reminders created through JPA.
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
