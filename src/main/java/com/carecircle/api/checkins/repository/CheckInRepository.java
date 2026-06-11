package com.carecircle.api.checkins.repository;

import com.carecircle.api.checkins.entity.CheckIn;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence access for care circle check-ins.
 */
public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    /**
     * Finds check-ins belonging to one care circle, eagerly loading users needed
     * by API responses.
     *
     * @param careCircleId care circle identifier.
     * @return check-ins in the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "createdByUser"})
    List<CheckIn> findByCareCircle_Id(UUID careCircleId);
}
