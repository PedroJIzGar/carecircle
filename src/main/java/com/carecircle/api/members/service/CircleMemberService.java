package com.carecircle.api.members.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.members.dto.AddCircleMemberRequest;
import com.carecircle.api.members.dto.CircleMemberResponse;
import com.carecircle.api.members.dto.UpdateCircleMemberRoleRequest;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.mapper.CircleMemberMapper;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.shared.audit.entity.AuditAction;
import com.carecircle.api.shared.audit.entity.AuditEntityType;
import com.carecircle.api.shared.audit.service.AuditLogService;
import com.carecircle.api.shared.exception.ResourceConflictException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for care circle member read workflows.
 */
@Service
@RequiredArgsConstructor
public class CircleMemberService {

    private static final String ADD_FORBIDDEN_MESSAGE = "Only the main caregiver can add care circle members.";
    private static final String UPDATE_FORBIDDEN_MESSAGE = "Only the main caregiver can update care circle member roles.";
    private static final String REMOVE_FORBIDDEN_MESSAGE = "Only the main caregiver can remove care circle members.";

    private final UserService userService;
    private final UserRepository userRepository;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final CircleMemberRepository circleMemberRepository;
    private final AuditLogService auditLogService;
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
        CircleMember savedMember = circleMemberRepository.save(newMember);
        auditLogService.record(
                currentUser,
                AuditAction.CIRCLE_MEMBER_ADDED,
                AuditEntityType.CIRCLE_MEMBER,
                savedMember.getId(),
                Map.of(
                        "careCircleId", careCircleId.toString(),
                        "targetUserId", targetUser.getId().toString(),
                        "role", savedMember.getRole().name()
                )
        );

        return circleMemberMapper.toResponse(savedMember);
    }

    /**
     * Updates a non-owner member role inside a care circle.
     *
     * <p>This endpoint intentionally avoids modifying MAIN_CAREGIVER membership.
     * Main caregiver transfer needs a dedicated workflow with stronger safety
     * rules.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param memberId requested membership identifier.
     * @param request validated role update request.
     * @return updated membership response.
     */
    @Transactional
    public CircleMemberResponse updateCircleMemberRole(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UUID memberId,
            UpdateCircleMemberRoleRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getMainCaregiverMembershipOrThrow(
                careCircleId,
                currentUser,
                UPDATE_FORBIDDEN_MESSAGE
        );

        CircleMember targetMember = circleMemberRepository.findByIdAndCareCircle_IdAndStatus(
                        memberId,
                        careCircleId,
                        CircleMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new ResourceNotFoundException("Care circle member not found."));

        if (targetMember.getRole() == CircleRole.MAIN_CAREGIVER) {
            throw new ResourceConflictException("Main caregiver role changes require a dedicated flow.");
        }

        CircleRole previousRole = targetMember.getRole();
        targetMember.setRole(request.role());
        CircleMember savedMember = circleMemberRepository.save(targetMember);
        auditLogService.record(
                currentUser,
                AuditAction.CIRCLE_MEMBER_ROLE_UPDATED,
                AuditEntityType.CIRCLE_MEMBER,
                savedMember.getId(),
                Map.of(
                        "careCircleId", careCircleId.toString(),
                        "targetUserId", savedMember.getUser().getId().toString(),
                        "previousRole", previousRole.name(),
                        "newRole", savedMember.getRole().name()
                )
        );

        return circleMemberMapper.toResponse(savedMember);
    }

    /**
     * Removes a regular member from a care circle using a soft delete.
     *
     * <p>The membership row is kept for traceability, but it stops being active
     * for authorization and list operations.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param memberId requested membership identifier.
     */
    @Transactional
    public void removeCircleMember(SupabaseUserClaims claims, UUID careCircleId, UUID memberId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getMainCaregiverMembershipOrThrow(
                careCircleId,
                currentUser,
                REMOVE_FORBIDDEN_MESSAGE
        );

        CircleMember targetMember = circleMemberRepository.findByIdAndCareCircle_IdAndStatus(
                        memberId,
                        careCircleId,
                        CircleMemberStatus.ACTIVE
                )
                .orElseThrow(() -> new ResourceNotFoundException("Care circle member not found."));

        if (targetMember.getRole() == CircleRole.MAIN_CAREGIVER) {
            throw new ResourceConflictException("Main caregiver removal requires a dedicated flow.");
        }

        targetMember.setStatus(CircleMemberStatus.REMOVED);
        targetMember.setRemovedAt(OffsetDateTime.now());
        CircleMember savedMember = circleMemberRepository.save(targetMember);
        auditLogService.record(
                currentUser,
                AuditAction.CIRCLE_MEMBER_REMOVED,
                AuditEntityType.CIRCLE_MEMBER,
                savedMember.getId(),
                Map.of(
                        "careCircleId", careCircleId.toString(),
                        "targetUserId", savedMember.getUser().getId().toString(),
                        "role", savedMember.getRole().name()
                )
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
