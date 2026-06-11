package com.carecircle.api.tasks.repository;

import com.carecircle.api.tasks.entity.CareTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence access for care circle tasks.
 */
public interface CareTaskRepository extends JpaRepository<CareTask, UUID> {
}
