package com.carecircle.api.medications.repository;

import com.carecircle.api.medications.entity.MedicationIntakeLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence access for medication intake logs.
 */
public interface MedicationIntakeLogRepository extends JpaRepository<MedicationIntakeLog, UUID> {

    /**
     * Finds intake logs belonging to one care circle, eagerly loading relations
     * needed by API responses.
     *
     * @param careCircleId care circle identifier.
     * @return intake logs in the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "reminder", "recordedByUser"})
    List<MedicationIntakeLog> findByCareCircle_Id(UUID careCircleId);
}
