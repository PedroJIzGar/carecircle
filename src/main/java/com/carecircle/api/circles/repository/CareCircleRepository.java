package com.carecircle.api.circles.repository;

import com.carecircle.api.circles.entity.CareCircle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence access for care circles.
 */
public interface CareCircleRepository extends JpaRepository<CareCircle, UUID> {
}
