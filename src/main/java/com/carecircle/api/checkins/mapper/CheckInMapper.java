package com.carecircle.api.checkins.mapper;

import com.carecircle.api.checkins.dto.CheckInResponse;
import com.carecircle.api.checkins.entity.CheckIn;
import com.carecircle.api.users.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps check-in entities to API DTOs.
 */
@Component
public class CheckInMapper {

    /**
     * Converts a persisted check-in into a response safe for care circle members.
     *
     * @param checkIn persisted care circle check-in.
     * @return public check-in response.
     */
    public CheckInResponse toResponse(CheckIn checkIn) {
        User createdByUser = checkIn.getCreatedByUser();

        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getCareCircle().getId(),
                checkIn.getStatus(),
                checkIn.getNote(),
                checkIn.getCheckedAt(),
                createdByUser.getId(),
                createdByUser.getFullName(),
                checkIn.getCreatedAt(),
                checkIn.getUpdatedAt()
        );
    }
}
