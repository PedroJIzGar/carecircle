package com.carecircle.api.members.service;

import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.shared.exception.ForbiddenOperationException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
import com.carecircle.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Reusable membership-based authorization checks for care circle resources.
 */
@Service
@RequiredArgsConstructor
public class CircleMembershipAccessService {

    private final CircleMemberRepository circleMemberRepository;

    /**
     * Returns the active membership for a user and circle.
     *
     * <p>Missing membership is treated as not found to avoid revealing private
     * family resources to users outside the circle.</p>
     *
     * @param careCircleId care circle identifier.
     * @param user authenticated internal user.
     * @return active membership.
     */
    public CircleMember getActiveMembershipOrThrow(UUID careCircleId, User user) {
        return circleMemberRepository.findByCareCircle_IdAndUser_IdAndStatus(
                        careCircleId,
                        user.getId(),
                        CircleMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new ResourceNotFoundException("Care circle not found."));
    }

    /**
     * Returns the active membership when the user is the main caregiver.
     *
     * @param careCircleId care circle identifier.
     * @param user authenticated internal user.
     * @param forbiddenMessage API message used when the user has another role.
     * @return active main caregiver membership.
     */
    public CircleMember getMainCaregiverMembershipOrThrow(
            UUID careCircleId,
            User user,
            String forbiddenMessage
    ) {
        CircleMember membership = getActiveMembershipOrThrow(careCircleId, user);
        if (membership.getRole() != CircleRole.MAIN_CAREGIVER) {
            throw new ForbiddenOperationException(forbiddenMessage);
        }
        return membership;
    }
}
