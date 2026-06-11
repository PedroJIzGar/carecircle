package com.carecircle.api.tasks.repository;

import com.carecircle.api.tasks.entity.CareTask;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for care circle tasks.
 */
public interface CareTaskRepository extends JpaRepository<CareTask, UUID> {

    /**
     * Finds tasks belonging to one care circle, eagerly loading users needed by
     * API responses.
     *
     * @param careCircleId care circle identifier.
     * @return tasks in the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "assignedToUser", "createdByUser", "completedByUser"})
    List<CareTask> findByCareCircle_Id(UUID careCircleId);

    /**
     * Finds one task by id and care circle id, eagerly loading relations needed
     * by authorization-safe responses.
     *
     * @param id task identifier.
     * @param careCircleId care circle identifier.
     * @return matching task when it belongs to the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "assignedToUser", "createdByUser", "completedByUser"})
    Optional<CareTask> findByIdAndCareCircle_Id(UUID id, UUID careCircleId);
}
