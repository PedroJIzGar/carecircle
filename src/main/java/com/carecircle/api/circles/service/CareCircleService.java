package com.carecircle.api.circles.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.circles.dto.CareCircleResponse;
import com.carecircle.api.circles.dto.CreateCareCircleRequest;
import com.carecircle.api.circles.entity.CareCircle;
import com.carecircle.api.circles.mapper.CareCircleMapper;
import com.carecircle.api.circles.repository.CareCircleRepository;
import com.carecircle.api.elderprofiles.entity.ElderProfile;
import com.carecircle.api.elderprofiles.repository.ElderProfileRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.repository.CircleMemberRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
