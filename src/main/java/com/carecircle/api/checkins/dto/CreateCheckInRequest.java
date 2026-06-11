package com.carecircle.api.checkins.dto;

import com.carecircle.api.checkins.entity.CheckInStatus;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Request body for creating a care circle check-in.
 *
 * @param status required non-clinical family status signal.
 * @param note optional family coordination note.
 * @param checkedAt optional time the check-in refers to. Defaults to now when absent.
 */
public record CreateCheckInRequest(
        @NotNull
        CheckInStatus status,

        @Size(max = 1000)
        String note,

        @PastOrPresent
        OffsetDateTime checkedAt
) {
}
