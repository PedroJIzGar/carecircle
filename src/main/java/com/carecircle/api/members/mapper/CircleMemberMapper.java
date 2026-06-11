package com.carecircle.api.members.mapper;

import com.carecircle.api.members.dto.CircleMemberResponse;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.users.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps circle membership entities to API DTOs.
 */
@Component
public class CircleMemberMapper {

    /**
     * Converts a persisted membership into a response safe for circle members.
     *
     * @param member persisted circle membership.
     * @return public member response.
     */
    public CircleMemberResponse toResponse(CircleMember member) {
        User user = member.getUser();
        return new CircleMemberResponse(
                member.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getCreatedAt()
        );
    }
}
