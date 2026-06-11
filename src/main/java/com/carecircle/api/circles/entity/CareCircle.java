package com.carecircle.api.circles.entity;

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
 * Family care space used to coordinate daily care around one elder profile.
 *
 * <p>A care circle owns the collaboration context. Users do not get family
 * roles directly; those roles are represented through CircleMember.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "care_circles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareCircle {

    /**
     * Internal care circle identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Human-readable circle name shown to family members.
     */
    @Column(name = "name", nullable = false, length = 160)
    private String name;

    /**
     * Optional short description for family context.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Current lifecycle state of the circle.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CareCircleStatus status = CareCircleStatus.ACTIVE;

    /**
     * User that created the circle.
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
     * Creates an active care circle.
     *
     * @param name family-visible circle name.
     * @param createdByUser internal user that creates the circle.
     */
    public CareCircle(String name, User createdByUser) {
        this.name = name;
        this.createdByUser = createdByUser;
        this.status = CareCircleStatus.ACTIVE;
    }

    /**
     * Ensures timestamps are present for new circles created through JPA.
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
