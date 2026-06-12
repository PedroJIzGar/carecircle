package com.carecircle.api.companionrequests.repository;

import com.carecircle.api.companionrequests.entity.CompanionRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for family companion requests.
 */
public interface CompanionRequestRepository extends JpaRepository<CompanionRequest, UUID> {

    /**
     * Finds companion requests belonging to one care circle.
     *
     * @param careCircleId care circle identifier.
     * @return companion requests in the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "requestedByUser", "partnerOrganization", "cancelledByUser"})
    List<CompanionRequest> findByCareCircle_Id(UUID careCircleId);

    /**
     * Finds one companion request by id and care circle id.
     *
     * @param id companion request identifier.
     * @param careCircleId care circle identifier.
     * @return matching request when it belongs to the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "requestedByUser", "partnerOrganization", "cancelledByUser"})
    Optional<CompanionRequest> findByIdAndCareCircle_Id(UUID id, UUID careCircleId);
}
