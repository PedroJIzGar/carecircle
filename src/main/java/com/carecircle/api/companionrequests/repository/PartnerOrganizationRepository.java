package com.carecircle.api.companionrequests.repository;

import com.carecircle.api.companionrequests.entity.PartnerOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence access for verified partner organizations.
 */
public interface PartnerOrganizationRepository extends JpaRepository<PartnerOrganization, UUID> {
}
