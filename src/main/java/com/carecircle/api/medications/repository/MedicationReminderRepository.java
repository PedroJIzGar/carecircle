package com.carecircle.api.medications.repository;

import com.carecircle.api.medications.entity.MedicationReminder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for family medication reminders.
 */
public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, UUID> {

    /**
     * Finds reminders belonging to one care circle, eagerly loading users needed
     * by API responses.
     *
     * @param careCircleId care circle identifier.
     * @return medication reminders in the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "createdByUser", "archivedByUser"})
    List<MedicationReminder> findByCareCircle_Id(UUID careCircleId);

    /**
     * Finds one reminder by id and care circle id.
     *
     * @param id reminder identifier.
     * @param careCircleId care circle identifier.
     * @return matching reminder when it belongs to the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "createdByUser", "archivedByUser"})
    Optional<MedicationReminder> findByIdAndCareCircle_Id(UUID id, UUID careCircleId);
}
