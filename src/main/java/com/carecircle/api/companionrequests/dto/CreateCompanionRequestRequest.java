package com.carecircle.api.companionrequests.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating a family companion request.
 *
 * @param requestedForDate required requested date.
 * @param timeWindow required preferred time window.
 * @param location required family-entered location or meeting context.
 * @param reason optional non-clinical request reason.
 * @param notes optional family coordination notes.
 */
public record CreateCompanionRequestRequest(
        @NotNull
        @FutureOrPresent
        LocalDate requestedForDate,

        @NotBlank
        @Size(max = 160)
        String timeWindow,

        @NotBlank
        @Size(max = 255)
        String location,

        @Size(max = 500)
        String reason,

        @Size(max = 1000)
        String notes
) {
}
