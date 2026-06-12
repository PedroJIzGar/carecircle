package com.carecircle.api.companionrequests.dto;

import com.carecircle.api.companionrequests.entity.CompanionRequestStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a family companion request.
 *
 * @param id internal companion request identifier.
 * @param careCircleId care circle identifier.
 * @param requestedByUserId internal user that created the request.
 * @param requestedByFullName display name of the creator.
 * @param status current request lifecycle state.
 * @param requestedForDate requested date.
 * @param timeWindow preferred time window.
 * @param location family-entered location or meeting context.
 * @param reason optional non-clinical request reason.
 * @param notes optional family coordination notes.
 * @param partnerOrganizationId optional verified partner organization for future referral workflows.
 * @param partnerOrganizationName optional verified partner organization name.
 * @param submittedToPartnerAt future workflow timestamp for partner referral.
 * @param cancelledAt cancellation timestamp, when cancelled.
 * @param cancelledByUserId internal user that cancelled the request, when cancelled.
 * @param createdAt entity creation timestamp.
 * @param updatedAt last update timestamp.
 */
public record CompanionRequestResponse(
        UUID id,
        UUID careCircleId,
        UUID requestedByUserId,
        String requestedByFullName,
        CompanionRequestStatus status,
        LocalDate requestedForDate,
        String timeWindow,
        String location,
        String reason,
        String notes,
        UUID partnerOrganizationId,
        String partnerOrganizationName,
        OffsetDateTime submittedToPartnerAt,
        OffsetDateTime cancelledAt,
        UUID cancelledByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
