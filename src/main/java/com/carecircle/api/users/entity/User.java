package com.carecircle.api.users.entity;

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
 * Internal CareCircle user synchronized from Supabase Auth.
 *
 * <p>This entity does not store credentials. Supabase remains the identity
 * provider, while this table stores the application-owned profile fields,
 * account lifecycle state and timestamps needed by CareCircle business rules.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /**
     * Internal CareCircle identifier used by all domain relationships.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Supabase Auth user identifier, sourced from the JWT {@code sub} claim.
     */
    @Column(name = "supabase_user_id", nullable = false, unique = true, length = 128)
    private String supabaseUserId;

    /**
     * User display name. This can be absent depending on Supabase metadata.
     */
    @Column(name = "full_name", length = 160)
    private String fullName;

    /**
     * Email address supplied by Supabase Auth.
     */
    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    /**
     * Optional phone number for future product flows.
     */
    @Column(name = "phone", length = 32)
    private String phone;

    /**
     * Optional profile image URL from Supabase user metadata.
     */
    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    /**
     * Application-level role. Family care roles belong to CircleMember, not User.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", nullable = false, length = 32)
    private GlobalRole globalRole = GlobalRole.USER;

    /**
     * Internal account lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 32)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    /**
     * Email verification state as reported by Supabase.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /**
     * Timestamp for accepting current terms of service, if tracked directly.
     */
    @Column(name = "terms_accepted_at")
    private OffsetDateTime termsAcceptedAt;

    /**
     * Timestamp for accepting the privacy policy, if tracked directly.
     */
    @Column(name = "privacy_accepted_at")
    private OffsetDateTime privacyAcceptedAt;

    /**
     * Timestamp for accepting the non-medical product disclaimer, if tracked directly.
     */
    @Column(name = "medical_disclaimer_accepted_at")
    private OffsetDateTime medicalDisclaimerAcceptedAt;

    /**
     * Entity creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last application-side update timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Last successful backend synchronization/login timestamp.
     */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /**
     * Creates a user with the defaults required for a Supabase-synchronized account.
     *
     * @param supabaseUserId Supabase Auth subject claim.
     * @param email email address from Supabase Auth.
     */
    public User(String supabaseUserId, String email) {
        this.supabaseUserId = supabaseUserId;
        this.email = email;
        this.globalRole = GlobalRole.USER;
        this.accountStatus = AccountStatus.ACTIVE;
    }

    /**
     * Ensures timestamps are present for new users created through JPA.
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
