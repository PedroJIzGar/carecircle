package com.carecircle.api.companionrequests.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Verified organization that may receive companion request referrals.
 *
 * <p>This entity prepares the backend for partner referral workflows. It does
 * not represent direct volunteer assignment.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "partner_organizations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartnerOrganization {

    /**
     * Internal partner organization identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Public organization name.
     */
    @Column(name = "name", nullable = false, length = 160)
    private String name;

    /**
     * Optional operational contact email.
     */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /**
     * Optional operational contact phone.
     */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /**
     * Current verification state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PartnerOrganizationStatus status = PartnerOrganizationStatus.VERIFIED;

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
     * Creates a verified partner organization.
     *
     * @param name organization name.
     */
    public PartnerOrganization(String name) {
        this.name = name;
        this.status = PartnerOrganizationStatus.VERIFIED;
    }

    /**
     * Ensures timestamps are present for new partner organizations created through JPA.
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
