package com.carecircle.api.medications.mapper;

import com.carecircle.api.medications.dto.MedicationReminderResponse;
import com.carecircle.api.medications.entity.MedicationReminder;
import com.carecircle.api.users.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps medication reminder entities to API DTOs.
 */
@Component
public class MedicationReminderMapper {

    /**
     * Converts a persisted reminder into a response safe for care circle members.
     *
     * @param reminder persisted family medication reminder.
     * @return public medication reminder response.
     */
    public MedicationReminderResponse toResponse(MedicationReminder reminder) {
        User createdByUser = reminder.getCreatedByUser();
        User archivedByUser = reminder.getArchivedByUser();

        return new MedicationReminderResponse(
                reminder.getId(),
                reminder.getCareCircle().getId(),
                reminder.getMedicationName(),
                reminder.getDosageText(),
                reminder.getScheduleText(),
                reminder.getInstructions(),
                reminder.getStatus(),
                reminder.getStartDate(),
                reminder.getEndDate(),
                createdByUser.getId(),
                createdByUser.getFullName(),
                reminder.getArchivedAt(),
                getUserId(archivedByUser),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }

    private UUID getUserId(User user) {
        return user == null ? null : user.getId();
    }
}
