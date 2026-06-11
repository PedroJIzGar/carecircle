package com.carecircle.api.appointments.repository;

import com.carecircle.api.appointments.entity.Appointment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for care circle appointments.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * Finds appointments belonging to one care circle, eagerly loading users
     * needed by API responses.
     *
     * @param careCircleId care circle identifier.
     * @return appointments in the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "createdByUser", "cancelledByUser"})
    List<Appointment> findByCareCircle_Id(UUID careCircleId);

    /**
     * Finds one appointment by id and care circle id, eagerly loading relations
     * needed by authorization-safe responses.
     *
     * @param id appointment identifier.
     * @param careCircleId care circle identifier.
     * @return matching appointment when it belongs to the requested circle.
     */
    @EntityGraph(attributePaths = {"careCircle", "createdByUser", "cancelledByUser"})
    Optional<Appointment> findByIdAndCareCircle_Id(UUID id, UUID careCircleId);
}
