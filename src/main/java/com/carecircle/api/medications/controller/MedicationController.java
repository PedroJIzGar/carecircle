package com.carecircle.api.medications.controller;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
import com.carecircle.api.medications.dto.CreateMedicationIntakeLogRequest;
import com.carecircle.api.medications.dto.CreateMedicationReminderRequest;
import com.carecircle.api.medications.dto.MedicationIntakeLogResponse;
import com.carecircle.api.medications.dto.MedicationReminderResponse;
import com.carecircle.api.medications.dto.UpdateMedicationReminderRequest;
import com.carecircle.api.medications.service.MedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Care circle medication reminder and intake log API endpoints.
 */
@RestController
@RequestMapping("/circles/{circleId}")
@RequiredArgsConstructor
@Tag(name = "Medications", description = "Family medication reminder and intake log endpoints")
public class MedicationController {

    private final CurrentUserProvider currentUserProvider;
    private final MedicationService medicationService;

    /**
     * Creates a family-entered medication reminder.
     *
     * @param circleId requested care circle identifier.
     * @param request validated medication reminder creation request.
     * @return created medication reminder response.
     */
    @PostMapping("/medication-reminders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a medication reminder",
            description = "Creates a family-entered medication reminder. CareCircle does not prescribe, recommend, or modify medication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public MedicationReminderResponse createReminder(
            @PathVariable UUID circleId,
            @Valid @RequestBody CreateMedicationReminderRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return medicationService.createReminder(claims, circleId, request);
    }

    /**
     * Lists medication reminders visible to the authenticated care circle member.
     *
     * @param circleId requested care circle identifier.
     * @return ordered medication reminders for the requested circle.
     */
    @GetMapping("/medication-reminders")
    @Operation(
            summary = "List medication reminders",
            description = "Returns family-entered medication reminders when the authenticated user is an active member of the care circle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<MedicationReminderResponse> listReminders(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return medicationService.listReminders(claims, circleId);
    }

    /**
     * Updates editable fields of an active medication reminder.
     *
     * @param circleId requested care circle identifier.
     * @param reminderId requested medication reminder identifier.
     * @param request validated medication reminder update request.
     * @return updated medication reminder response.
     */
    @PatchMapping("/medication-reminders/{reminderId}")
    @Operation(
            summary = "Update a medication reminder",
            description = "Updates family-entered reminder fields while the reminder is ACTIVE. CareCircle does not validate clinical dosage or schedule correctness.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public MedicationReminderResponse updateReminder(
            @PathVariable UUID circleId,
            @PathVariable UUID reminderId,
            @Valid @RequestBody UpdateMedicationReminderRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return medicationService.updateReminder(claims, circleId, reminderId, request);
    }

    /**
     * Archives an active medication reminder.
     *
     * @param circleId requested care circle identifier.
     * @param reminderId requested medication reminder identifier.
     * @return archived medication reminder response.
     */
    @PostMapping("/medication-reminders/{reminderId}/archive")
    @Operation(
            summary = "Archive a medication reminder",
            description = "Archives a family-entered medication reminder without deleting its intake log history.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public MedicationReminderResponse archiveReminder(
            @PathVariable UUID circleId,
            @PathVariable UUID reminderId
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return medicationService.archiveReminder(claims, circleId, reminderId);
    }

    /**
     * Creates a manual medication intake log.
     *
     * @param circleId requested care circle identifier.
     * @param request validated intake log creation request.
     * @return created medication intake log response.
     */
    @PostMapping("/medication-intake-logs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a medication intake log",
            description = "Creates a manual family-recorded intake log for an active reminder. CareCircle does not decide whether medication should be taken or skipped.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public MedicationIntakeLogResponse createIntakeLog(
            @PathVariable UUID circleId,
            @Valid @RequestBody CreateMedicationIntakeLogRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return medicationService.createIntakeLog(claims, circleId, request);
    }

    /**
     * Lists medication intake logs visible to the authenticated care circle member.
     *
     * @param circleId requested care circle identifier.
     * @return ordered medication intake logs for the requested circle.
     */
    @GetMapping("/medication-intake-logs")
    @Operation(
            summary = "List medication intake logs",
            description = "Returns manual medication intake logs when the authenticated user is an active member of the care circle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<MedicationIntakeLogResponse> listIntakeLogs(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return medicationService.listIntakeLogs(claims, circleId);
    }
}
