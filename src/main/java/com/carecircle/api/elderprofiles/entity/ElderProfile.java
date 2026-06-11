package com.carecircle.api.elderprofiles.entity;

import com.carecircle.api.circles.entity.CareCircle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
 * Basic non-clinical profile for the elder person cared for in a circle.
 *
 * <p>This entity intentionally avoids diagnosis, treatment and medication
 * decision fields. Medical reminders and logs are modeled in dedicated MVP
 * modules without turning CareCircle into a medical decision system.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "elder_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderProfile {

    /**
     * Internal elder profile identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Care circle that owns this elder profile.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "care_circle_id", nullable = false, unique = true)
    private CareCircle careCircle;

    /**
     * Full name of the elder person.
     */
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    /**
     * Optional preferred name used by the family.
     */
    @Column(name = "preferred_name", length = 100)
    private String preferredName;

    /**
     * Optional birth date. It can be absent when the family does not provide it.
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /**
     * General family notes. This must not become a clinical record.
     */
    @Column(name = "notes", length = 1000)
    private String notes;

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
     * Creates an elder profile for a care circle.
     *
     * @param careCircle owning care circle.
     * @param fullName full name of the elder person.
     */
    public ElderProfile(CareCircle careCircle, String fullName) {
        this.careCircle = careCircle;
        this.fullName = fullName;
    }

    /**
     * Ensures timestamps are present for new profiles created through JPA.
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
