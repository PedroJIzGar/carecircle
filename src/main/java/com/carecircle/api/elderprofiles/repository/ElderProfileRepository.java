package com.carecircle.api.elderprofiles.repository;

import com.carecircle.api.elderprofiles.entity.ElderProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for elder profiles.
 */
public interface ElderProfileRepository extends JpaRepository<ElderProfile, UUID> {

    /**
     * Finds the elder profile owned by a care circle.
     *
     * @param careCircleId care circle identifier.
     * @return matching elder profile when present.
     */
    Optional<ElderProfile> findByCareCircle_Id(UUID careCircleId);
}
