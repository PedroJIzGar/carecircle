package com.carecircle.api.elderprofiles.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.circles.dto.CareCircleResponse;
import com.carecircle.api.circles.mapper.CareCircleMapper;
import com.carecircle.api.elderprofiles.dto.UpdateElderProfileRequest;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Application service for elder profile workflows.
 */
@Service
@RequiredArgsConstructor
public class ElderProfileService {

    private static final String UPDATE_FORBIDDEN_MESSAGE = "Only the main caregiver can update the elder profile.";

    private final UserService userService;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final ElderProfileRepository elderProfileRepository;
    private final CareCircleMapper careCircleMapper;

    /**
     * Updates the basic non-clinical elder profile for a care circle.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated partial update request.
     * @return updated care circle aggregate.
     */
    @Transactional
    public CareCircleResponse updateElderProfile(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UpdateElderProfileRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember membership = circleMembershipAccessService.getMainCaregiverMembershipOrThrow(
                careCircleId,
                currentUser,
                UPDATE_FORBIDDEN_MESSAGE
        );

        ElderProfile elderProfile = elderProfileRepository.findByCareCircle_Id(careCircleId)
                .orElseThrow(() -> new IllegalStateException("Care circle is missing its elder profile."));

        if (request.fullName() != null) {
            elderProfile.setFullName(normalizeRequired(request.fullName()));
        }
        if (request.preferredName() != null) {
            elderProfile.setPreferredName(normalizeOptional(request.preferredName()));
        }
        if (request.birthDate() != null) {
            elderProfile.setBirthDate(request.birthDate());
        }
        if (request.notes() != null) {
            elderProfile.setNotes(normalizeOptional(request.notes()));
        }

        ElderProfile savedElderProfile = elderProfileRepository.save(elderProfile);
        return careCircleMapper.toResponse(membership.getCareCircle(), savedElderProfile, membership);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
