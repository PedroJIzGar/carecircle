package com.carecircle.api.appointments.mapper;

import com.carecircle.api.appointments.dto.AppointmentResponse;
import com.carecircle.api.appointments.entity.Appointment;
import com.carecircle.api.users.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps appointment entities to API DTOs.
 */
@Component
public class AppointmentMapper {

    /**
     * Converts a persisted appointment into a response safe for care circle members.
     *
     * @param appointment persisted care circle appointment.
     * @return public appointment response.
     */
    public AppointmentResponse toResponse(Appointment appointment) {
        User createdByUser = appointment.getCreatedByUser();
        User cancelledByUser = appointment.getCancelledByUser();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCareCircle().getId(),
                appointment.getTitle(),
                appointment.getLocation(),
                appointment.getNotes(),
                appointment.getStatus(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                createdByUser.getId(),
                createdByUser.getFullName(),
                appointment.getCancelledAt(),
                getUserId(cancelledByUser),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }

    private UUID getUserId(User user) {
        return user == null ? null : user.getId();
    }
}
