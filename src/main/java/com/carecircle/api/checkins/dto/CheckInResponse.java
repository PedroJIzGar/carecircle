package com.carecircle.api.checkins.dto;

import com.carecircle.api.checkins.entity.CheckInStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for a care circle check-in.
 *
 * @param id internal check-in identifier.
 * @param careCircleId care circle identifier.
 * @param status non-clinical family status signal.
 * @param note optional family coordination note.
 * @param checkedAt time the check-in refers to.
 * @param createdByUserId internal user that created the check-in.
 * @param createdByFullName display name of the creator.
 * @param createdAt entity creation timestamp.
 * @param updatedAt last update timestamp.
 */
public record CheckInResponse(
        UUID id,
        UUID careCircleId,
        CheckInStatus status,
        String note,
        OffsetDateTime checkedAt,
        UUID createdByUserId,
        String createdByFullName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
