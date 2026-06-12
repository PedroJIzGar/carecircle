package com.carecircle.api.companionrequests.mapper;

import com.carecircle.api.companionrequests.dto.CompanionRequestResponse;
import com.carecircle.api.companionrequests.entity.CompanionRequest;
import com.carecircle.api.companionrequests.entity.PartnerOrganization;
import com.carecircle.api.users.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps companion request entities to API DTOs.
 */
@Component
public class CompanionRequestMapper {

    /**
     * Converts a persisted companion request into a response safe for care circle members.
     *
     * @param request persisted companion request.
     * @return public companion request response.
     */
    public CompanionRequestResponse toResponse(CompanionRequest request) {
        User requestedByUser = request.getRequestedByUser();
        PartnerOrganization partnerOrganization = request.getPartnerOrganization();
        User cancelledByUser = request.getCancelledByUser();

        return new CompanionRequestResponse(
                request.getId(),
                request.getCareCircle().getId(),
                requestedByUser.getId(),
                requestedByUser.getFullName(),
                request.getStatus(),
                request.getRequestedForDate(),
                request.getTimeWindow(),
                request.getLocation(),
                request.getReason(),
                request.getNotes(),
                getPartnerOrganizationId(partnerOrganization),
                getPartnerOrganizationName(partnerOrganization),
                request.getSubmittedToPartnerAt(),
                request.getCancelledAt(),
                getUserId(cancelledByUser),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private UUID getPartnerOrganizationId(PartnerOrganization partnerOrganization) {
        return partnerOrganization == null ? null : partnerOrganization.getId();
    }

    private String getPartnerOrganizationName(PartnerOrganization partnerOrganization) {
        return partnerOrganization == null ? null : partnerOrganization.getName();
    }

    private UUID getUserId(User user) {
        return user == null ? null : user.getId();
    }
}
