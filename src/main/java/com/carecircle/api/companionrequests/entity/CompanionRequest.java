package com.carecircle.api.companionrequests.entity;

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
 * Family request for companion support that may be referred to verified partners.
 *
 * <p>CareCircle records and organizes these requests. It does not assign
 * volunteers directly.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "companion_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionRequest {

    /**
     * Internal companion request identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle where the request belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false)
    private CareCircle careCircle;

    /**
     * User that created the companion request.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    /**
     * Current request lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CompanionRequestStatus status = CompanionRequestStatus.REQUESTED;

    /**
     * Date requested by the family.
     */
    @Column(name = "requested_for_date", nullable = false)
    private LocalDate requestedForDate;

    /**
     * Family-entered preferred time window.
     */
    @Column(name = "time_window", nullable = false, length = 160)
    private String timeWindow;

    /**
     * Family-entered location or meeting context.
     */
    @Column(name = "location", nullable = false, length = 255)
    private String location;

    /**
     * Optional non-clinical request reason.
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Optional family coordination notes.
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Optional verified partner organization for future referral workflows.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_organization_id")
    private PartnerOrganization partnerOrganization;

    /**
     * Future workflow timestamp for partner referral.
     */
    @Column(name = "submitted_to_partner_at")
    private OffsetDateTime submittedToPartnerAt;

    /**
     * Cancellation timestamp, present only for cancelled requests.
     */
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    /**
     * User that cancelled the request, present only for cancelled requests.
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
     * Creates an active companion request.
     *
     * @param careCircle circle that owns the request.
     * @param requestedByUser user creating the request.
     * @param requestedForDate requested date.
     * @param timeWindow preferred time window.
     * @param location family-entered location.
     */
    public CompanionRequest(
            CareCircle careCircle,
            User requestedByUser,
            LocalDate requestedForDate,
            String timeWindow,
            String location
    ) {
        this.careCircle = careCircle;
        this.requestedByUser = requestedByUser;
        this.requestedForDate = requestedForDate;
        this.timeWindow = timeWindow;
        this.location = location;
        this.status = CompanionRequestStatus.REQUESTED;
    }

    /**
     * Ensures timestamps are present for new companion requests created through JPA.
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
