package com.carecircle.api.users.repository;

import com.carecircle.api.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for internal CareCircle users.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds an internal user by the Supabase Auth subject claim.
     *
     * @param supabaseUserId Supabase Auth user identifier.
     * @return matching user when it has already been synchronized.
     */
    Optional<User> findBySupabaseUserId(String supabaseUserId);

    /**
     * Finds an internal user by email.
     *
     * @param email email address supplied by Supabase Auth.
     * @return matching user when present.
     */
    Optional<User> findByEmail(String email);
}
