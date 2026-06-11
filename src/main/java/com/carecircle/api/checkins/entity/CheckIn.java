package com.carecircle.api.checkins.entity;

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
 * Non-clinical family check-in used to coordinate day-to-day care awareness.
 *
 * <p>Check-ins are intentionally lightweight family updates. They must not be
 * used to diagnose, recommend treatments, or modify medication.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "checkins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckIn {

    /**
     * Internal check-in identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle where the check-in belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false)
    private CareCircle careCircle;

    /**
     * Non-clinical family status signal.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CheckInStatus status;

    /**
     * Optional family coordination note.
     */
    @Column(name = "note", length = 1000)
    private String note;

    /**
     * Time the family check-in refers to.
     */
    @Column(name = "checked_at", nullable = false)
    private OffsetDateTime checkedAt;

    /**
     * User that created the check-in.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

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
     * Creates a family check-in inside a care circle.
     *
     * @param careCircle circle that owns the check-in.
     * @param status non-clinical family status signal.
     * @param checkedAt time the check-in refers to.
     * @param createdByUser user creating the check-in.
     */
    public CheckIn(CareCircle careCircle, CheckInStatus status, OffsetDateTime checkedAt, User createdByUser) {
        this.careCircle = careCircle;
        this.status = status;
        this.checkedAt = checkedAt;
        this.createdByUser = createdByUser;
    }

    /**
     * Ensures timestamps are present for new check-ins created through JPA.
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
