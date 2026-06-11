package com.carecircle.api.circles.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.circles.dto.CareCircleResponse;
import com.carecircle.api.circles.dto.CreateCareCircleRequest;
import com.carecircle.api.circles.dto.UpdateCareCircleRequest;
import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.mapper.CareCircleMapper;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleMemberStatus;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for care circle workflows.
 */
@Service
@RequiredArgsConstructor
public class CareCircleService {

    private final UserService userService;
    private final CareCircleRepository careCircleRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final CircleMemberRepository circleMemberRepository;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final CareCircleMapper careCircleMapper;

    /**
     * Creates a care circle aggregate for the authenticated Supabase user.
     *
     * <p>The operation is transactional: care circle, elder profile and main
     * caregiver membership are committed together or rolled back together.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param request validated request body.
     * @return created care circle aggregate response.
     */
    @Transactional
    public CareCircleResponse createCareCircle(
            SupabaseUserClaims claims,
            CreateCareCircleRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);

        CareCircle careCircle = new CareCircle(normalizeRequired(request.circle().name()), currentUser);
        careCircle.setDescription(normalizeOptional(request.circle().description()));
        CareCircle savedCareCircle = careCircleRepository.save(careCircle);

        ElderProfile elderProfile = new ElderProfile(
                savedCareCircle,
                normalizeRequired(request.elderProfile().fullName())
        );
        elderProfile.setPreferredName(normalizeOptional(request.elderProfile().preferredName()));
        elderProfile.setBirthDate(request.elderProfile().birthDate());
        elderProfile.setNotes(normalizeOptional(request.elderProfile().notes()));
        ElderProfile savedElderProfile = elderProfileRepository.save(elderProfile);

        CircleMember currentMembership = circleMemberRepository.save(
                new CircleMember(savedCareCircle, currentUser, CircleRole.MAIN_CAREGIVER)
        );

        return careCircleMapper.toResponse(savedCareCircle, savedElderProfile, currentMembership);
    }

    /**
     * Lists care circles visible to the authenticated Supabase user.
     *
     * <p>Visibility is based on active circle membership, not on circle ownership.
     * This keeps the authorization model ready for collaborators and observers.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @return care circles where the current user has an active membership.
     */
    @Transactional
    public List<CareCircleResponse> listCurrentUserCareCircles(SupabaseUserClaims claims) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);

        List<CircleMember> memberships = circleMemberRepository.findByUser_IdAndStatusOrderByCreatedAtAsc(
                currentUser.getId(),
                CircleMemberStatus.ACTIVE
        );

        if (memberships.isEmpty()) {
            return List.of();
        }

        List<UUID> careCircleIds = memberships.stream()
                .map(membership -> membership.getCareCircle().getId())
                .toList();

        Map<UUID, ElderProfile> elderProfilesByCircleId = elderProfileRepository.findByCareCircle_IdIn(careCircleIds)
                .stream()
                .collect(Collectors.toMap(
                        elderProfile -> elderProfile.getCareCircle().getId(),
                        Function.identity()
                ));

        return memberships.stream()
                .map(membership -> careCircleMapper.toResponse(
                        membership.getCareCircle(),
                        getRequiredElderProfile(elderProfilesByCircleId, membership),
                        membership
                ))
                .toList();
    }

    /**
     * Returns one care circle if the authenticated user has active membership.
     *
     * <p>A missing membership is treated as not found to avoid exposing whether
     * a care circle exists to users who cannot access it.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return requested care circle aggregate.
     */
    @Transactional
    public CareCircleResponse getCurrentUserCareCircle(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember membership = circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        ElderProfile elderProfile = elderProfileRepository.findByCareCircle_Id(careCircleId)
                .orElseThrow(() -> new IllegalStateException("Care circle is missing its elder profile."));

        return careCircleMapper.toResponse(membership.getCareCircle(), elderProfile, membership);
    }

    /**
     * Updates care circle basics when the current user is the main caregiver.
     *
     * <p>Reading is allowed for any active member, but changing circle-level
     * data is restricted to MAIN_CAREGIVER for the MVP.</p>
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated partial update request.
     * @return updated care circle aggregate.
     */
    @Transactional
    public CareCircleResponse updateCareCircle(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UpdateCareCircleRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember membership = circleMembershipAccessService.getMainCaregiverMembershipOrThrow(
                careCircleId,
                currentUser,
                "Only the main caregiver can update care circle details."
        );

        CareCircle careCircle = membership.getCareCircle();
        if (request.name() != null) {
            careCircle.setName(normalizeRequired(request.name()));
        }
        if (request.description() != null) {
            careCircle.setDescription(normalizeOptional(request.description()));
        }

        CareCircle savedCareCircle = careCircleRepository.save(careCircle);
        ElderProfile elderProfile = elderProfileRepository.findByCareCircle_Id(careCircleId)
                .orElseThrow(() -> new IllegalStateException("Care circle is missing its elder profile."));

        return careCircleMapper.toResponse(savedCareCircle, elderProfile, membership);
    }

    private ElderProfile getRequiredElderProfile(
            Map<UUID, ElderProfile> elderProfilesByCircleId,
            CircleMember membership
    ) {
        UUID careCircleId = membership.getCareCircle().getId();
        ElderProfile elderProfile = elderProfilesByCircleId.get(careCircleId);
        if (elderProfile == null) {
            throw new IllegalStateException("Care circle is missing its elder profile.");
        }
        return elderProfile;
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
