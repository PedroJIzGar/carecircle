package com.carecircle.api.users.mapper;

import com.carecircle.api.users.dto.UserResponse;
import com.carecircle.api.users.entity.User;
import org.mapstruct.Mapper;

/**
 * Maps user domain entities to API DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a persisted user entity into the public API response shape.
     *
     * @param user persisted user entity.
     * @return DTO safe to expose through authenticated API endpoints.
     */
    UserResponse toResponse(User user);
}
