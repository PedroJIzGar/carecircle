package com.carecircle.api.members.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.members.dto.AddCircleMemberRequest;
import com.carecircle.api.members.dto.CircleMemberResponse;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.mapper.CircleMemberMapper;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.shared.exception.ResourceConflictException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Application service for care circle member read workflows.
 */
@Service
@RequiredArgsConstructor
public class CircleMemberService {

    private static final String ADD_FORBIDDEN_MESSAGE = "Only the main caregiver can add care circle members.";

    private final UserService userService;
    private final UserRepository userRepository;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final CircleMemberRepository circleMemberRepository;
    private final CircleMemberMapper circleMemberMapper;

    /**
     * Lists active members in a care circle visible to the authenticated user.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return active members in the circle.
     */
    @Transactional(readOnly = true)
    public List<CircleMemberResponse> listCircleMembers(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        return circleMemberRepository.findByCareCircle_IdAndStatusOrderByCreatedAtAsc(
                        careCircleId,
                        CircleMemberStatus.ACTIVE
                )
                .stream()
                .map(circleMemberMapper::toResponse)
                .toList();
    }

    /**
     * Adds an existing internal user to a care circle.
     *
     * <p>This is not a full invitation flow. The target user must already have
     * a CareCircle user row synchronized from Supabase Auth.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated add member request.
     * @return created membership response.
     */
    @Transactional
    public CircleMemberResponse addCircleMember(
            SupabaseUserClaims claims,
            UUID careCircleId,
            AddCircleMemberRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getMainCaregiverMembershipOrThrow(
                careCircleId,
                currentUser,
                ADD_FORBIDDEN_MESSAGE
        );

        String targetEmail = normalizeEmail(request.email());
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (circleMemberRepository.existsByCareCircle_IdAndUser_Id(careCircleId, targetUser.getId())) {
            throw new ResourceConflictException("User is already associated with this care circle.");
        }

        CircleMember newMember = new CircleMember(currentMembership.getCareCircle(), targetUser, request.role());
        return circleMemberMapper.toResponse(circleMemberRepository.save(newMember));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
