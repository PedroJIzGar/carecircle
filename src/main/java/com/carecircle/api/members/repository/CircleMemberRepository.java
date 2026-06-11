package com.carecircle.api.members.repository;

import com.carecircle.api.members.entity.CircleMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for care circle memberships.
 */
public interface CircleMemberRepository extends JpaRepository<CircleMember, UUID> {

    /**
     * Finds a user's membership inside a care circle.
     *
     * @param careCircleId care circle identifier.
     * @param userId internal user identifier.
     * @return matching membership when present.
     */
    Optional<CircleMember> findByCareCircle_IdAndUser_Id(UUID careCircleId, UUID userId);

    /**
     * Checks whether a user already belongs to a care circle.
     *
     * @param careCircleId care circle identifier.
     * @param userId internal user identifier.
     * @return true when a membership already exists.
     */
    boolean existsByCareCircle_IdAndUser_Id(UUID careCircleId, UUID userId);
}
