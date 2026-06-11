package com.carecircle.api.circles.mapper;

import com.carecircle.api.circles.dto.CareCircleResponse;
import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.members.entity.CircleMember;
import org.springframework.stereotype.Component;

/**
 * Maps care circle aggregate entities to API DTOs.
 *
 * <p>This mapper is implemented manually because the response combines three
 * entities into one aggregate DTO.</p>
 */
@Component
public class CareCircleMapper {

    /**
     * Converts a persisted care circle aggregate into the response returned by the API.
     *
     * @param careCircle persisted care circle.
     * @param elderProfile persisted elder profile.
     * @param currentMembership persisted membership for the authenticated user.
     * @return API response safe to expose to the authenticated client.
     */
    public CareCircleResponse toResponse(
            CareCircle careCircle,
            ElderProfile elderProfile,
            CircleMember currentMembership
    ) {
        return new CareCircleResponse(
                careCircle.getId(),
                careCircle.getName(),
                careCircle.getDescription(),
                careCircle.getStatus(),
                careCircle.getCreatedByUser().getId(),
                careCircle.getCreatedAt(),
                careCircle.getUpdatedAt(),
                new CareCircleResponse.ElderProfileResponse(
                        elderProfile.getId(),
                        elderProfile.getFullName(),
                        elderProfile.getPreferredName(),
                        elderProfile.getBirthDate(),
                        elderProfile.getNotes()
                ),
                new CareCircleResponse.CircleMemberResponse(
                        currentMembership.getId(),
                        currentMembership.getUser().getId(),
                        currentMembership.getRole(),
                        currentMembership.getStatus(),
                        currentMembership.getJoinedAt()
                )
        );
    }
}
