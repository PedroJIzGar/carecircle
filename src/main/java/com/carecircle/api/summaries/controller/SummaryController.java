package com.carecircle.api.summaries.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.summaries.dto.WeeklySummaryResponse;
import com.carecircle.api.summaries.service.WeeklySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Care circle summary API endpoints.
 */
@RestController
@RequestMapping("/circles/{circleId}/summaries")
@RequiredArgsConstructor
@Tag(name = "Summaries", description = "Care circle non-clinical summary endpoints")
public class SummaryController {

    private final CurrentUserProvider currentUserProvider;
    private final WeeklySummaryService weeklySummaryService;

    /**
     * Returns a non-clinical weekly summary for a care circle.
     *
     * @param circleId requested care circle identifier.
     * @param weekStart optional Monday that starts the requested week.
     * @return weekly summary counters.
     */
    @GetMapping("/weekly")
    @Operation(
            summary = "Get weekly care circle summary",
            description = "Returns non-clinical weekly counters derived from tasks, appointments, check-ins, and medication intake logs.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public WeeklySummaryResponse getWeeklySummary(
            @PathVariable UUID circleId,
            @Parameter(description = "Monday that starts the requested week. Defaults to the current week.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return weeklySummaryService.getWeeklySummary(claims, circleId, weekStart);
    }
}
