package com.carecircle.api.members.entity;

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
 * Membership that connects a user to a care circle with a circle-scoped role.
 *
 * <p>The same user can have different roles in different circles in the future.
 * MVP service rules can still restrict that if product scope requires it.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "circle_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CircleMember {

    /**
     * Internal membership identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle where the membership applies.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false)
    private CareCircle careCircle;

    /**
     * Internal user that belongs to the circle.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * User's family role inside this circle.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private CircleRole role;

    /**
     * Current membership lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CircleMemberStatus status = CircleMemberStatus.ACTIVE;

    /**
     * Timestamp for invitation creation, used by future invitation flows.
     */
    @Column(name = "invited_at")
    private OffsetDateTime invitedAt;

    /**
     * Timestamp for joining the circle.
     */
    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    /**
     * Timestamp for removal from the circle.
     */
    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

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
     * Creates an active circle membership.
     *
     * @param careCircle care circle where the user is added.
     * @param user internal user added to the circle.
     * @param role family role granted inside the circle.
     */
    public CircleMember(CareCircle careCircle, User user, CircleRole role) {
        this.careCircle = careCircle;
        this.user = user;
        this.role = role;
        this.status = CircleMemberStatus.ACTIVE;
        this.joinedAt = OffsetDateTime.now();
    }

    /**
     * Ensures timestamps are present for new memberships created through JPA.
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
