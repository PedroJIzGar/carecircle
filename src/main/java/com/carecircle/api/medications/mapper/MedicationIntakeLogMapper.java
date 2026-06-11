package com.carecircle.api.medications.mapper;

import com.carecircle.api.medications.dto.MedicationIntakeLogResponse;
import com.carecircle.api.medications.entity.MedicationIntakeLog;
import com.carecircle.api.medications.entity.MedicationReminder;
import com.carecircle.api.users.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps medication intake log entities to API DTOs.
 */
@Component
public class MedicationIntakeLogMapper {

    /**
     * Converts a persisted intake log into a response safe for care circle members.
     *
     * @param intakeLog persisted medication intake log.
     * @return public intake log response.
     */
    public MedicationIntakeLogResponse toResponse(MedicationIntakeLog intakeLog) {
        MedicationReminder reminder = intakeLog.getReminder();
        User recordedByUser = intakeLog.getRecordedByUser();

        return new MedicationIntakeLogResponse(
                intakeLog.getId(),
                intakeLog.getCareCircle().getId(),
                reminder.getId(),
                reminder.getMedicationName(),
                intakeLog.getStatus(),
                intakeLog.getOccurredAt(),
                intakeLog.getNote(),
                recordedByUser.getId(),
                recordedByUser.getFullName(),
                intakeLog.getCreatedAt(),
                intakeLog.getUpdatedAt()
        );
    }
}
