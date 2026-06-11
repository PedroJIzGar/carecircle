package com.carecircle.api.appointments.controller;

import com.carecircle.api.appointments.dto.AppointmentResponse;
import com.carecircle.api.appointments.dto.CreateAppointmentRequest;
import com.carecircle.api.appointments.dto.UpdateAppointmentRequest;
import com.carecircle.api.appointments.service.AppointmentService;
import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.auth.security.CurrentUserProvider;
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
 * Care circle appointment API endpoints.
 */
@RestController
@RequestMapping("/circles/{circleId}/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Care circle appointment coordination endpoints")
public class AppointmentController {

    private final CurrentUserProvider currentUserProvider;
    private final AppointmentService appointmentService;

    /**
     * Creates a non-clinical appointment in a care circle.
     *
     * @param circleId requested care circle identifier.
     * @param request validated appointment creation request.
     * @return created appointment response.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a care circle appointment",
            description = "Creates a SCHEDULED non-clinical appointment when the authenticated user is MAIN_CAREGIVER or COLLABORATOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public AppointmentResponse createAppointment(
            @PathVariable UUID circleId,
            @Valid @RequestBody CreateAppointmentRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return appointmentService.createAppointment(claims, circleId, request);
    }

    /**
     * Lists appointments visible to the authenticated member of a care circle.
     *
     * @param circleId requested care circle identifier.
     * @return ordered appointments for the requested circle.
     */
    @GetMapping
    @Operation(
            summary = "List care circle appointments",
            description = "Returns appointments when the authenticated user is an active member of the care circle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<AppointmentResponse> listAppointments(@PathVariable UUID circleId) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return appointmentService.listAppointments(claims, circleId);
    }

    /**
     * Updates editable fields of a scheduled care circle appointment.
     *
     * @param circleId requested care circle identifier.
     * @param appointmentId requested appointment identifier.
     * @param request validated appointment update request.
     * @return updated appointment response.
     */
    @PatchMapping("/{appointmentId}")
    @Operation(
            summary = "Update a care circle appointment",
            description = "Updates editable fields of a SCHEDULED appointment when the authenticated user is MAIN_CAREGIVER or COLLABORATOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public AppointmentResponse updateAppointment(
            @PathVariable UUID circleId,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody UpdateAppointmentRequest request
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return appointmentService.updateAppointment(claims, circleId, appointmentId, request);
    }

    /**
     * Cancels a scheduled care circle appointment.
     *
     * @param circleId requested care circle identifier.
     * @param appointmentId requested appointment identifier.
     * @return cancelled appointment response.
     */
    @PostMapping("/{appointmentId}/cancel")
    @Operation(
            summary = "Cancel a care circle appointment",
            description = "Marks a SCHEDULED appointment as CANCELLED when the authenticated user is MAIN_CAREGIVER or COLLABORATOR.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public AppointmentResponse cancelAppointment(
            @PathVariable UUID circleId,
            @PathVariable UUID appointmentId
    ) {
        SupabaseUserClaims claims = currentUserProvider.getRequiredClaims();
        return appointmentService.cancelAppointment(claims, circleId, appointmentId);
    }
}
