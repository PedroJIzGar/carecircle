package com.carecircle.api.members.repository;

import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
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

    /**
     * Finds memberships visible to a user, eagerly loading the circle data needed
     * by list responses.
     *
     * @param userId internal user identifier.
     * @param status membership status filter.
     * @return ordered memberships for the user.
     */
    @EntityGraph(attributePaths = {"careCircle", "careCircle.createdByUser", "user"})
    List<CircleMember> findByUser_IdAndStatusOrderByCreatedAtAsc(UUID userId, CircleMemberStatus status);

    /**
     * Finds one active membership for resource-level authorization checks.
     *
     * @param careCircleId care circle identifier.
     * @param userId internal user identifier.
     * @param status expected membership status.
     * @return membership when the user can access the circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "careCircle.createdByUser", "user"})
    Optional<CircleMember> findByCareCircle_IdAndUser_IdAndStatus(
            UUID careCircleId,
            UUID userId,
            CircleMemberStatus status
    );

    /**
     * Finds active members for one care circle, eagerly loading the user data
     * needed by member list responses.
     *
     * @param careCircleId care circle identifier.
     * @param status membership status filter.
     * @return ordered memberships in the circle.
     */
    @EntityGraph(attributePaths = {"user"})
    List<CircleMember> findByCareCircle_IdAndStatusOrderByCreatedAtAsc(
            UUID careCircleId,
            CircleMemberStatus status
    );
}
