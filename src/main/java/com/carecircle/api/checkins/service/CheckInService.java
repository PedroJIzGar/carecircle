package com.carecircle.api.checkins.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.checkins.dto.CheckInResponse;
import com.carecircle.api.checkins.dto.CreateCheckInRequest;
import com.carecircle.api.checkins.entity.CheckIn;
import com.carecircle.api.checkins.mapper.CheckInMapper;
import com.carecircle.api.checkins.repository.CheckInRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.shared.exception.ForbiddenOperationException;
import com.carecircle.api.shared.exception.InvalidRequestException;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Application service for care circle check-in workflows.
 */
@Service
@RequiredArgsConstructor
public class CheckInService {

    private static final String CREATE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can create care circle check-ins.";
    private static final Comparator<CheckIn> CHECK_IN_LIST_ORDER = Comparator
            .comparing(CheckIn::getCheckedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(CheckIn::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final UserService userService;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final CheckInRepository checkInRepository;
    private final CheckInMapper checkInMapper;

    /**
     * Creates a non-clinical family check-in inside a care circle.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated check-in creation request.
     * @return created check-in response.
     */
    @Transactional
    public CheckInResponse createCheckIn(SupabaseUserClaims claims, UUID careCircleId, CreateCheckInRequest request) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = circleMembershipAccessService.getActiveMembershipOrThrow(
                careCircleId,
                currentUser
        );

        if (currentMembership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(CREATE_FORBIDDEN_MESSAGE);
        }

        OffsetDateTime checkedAt = resolveCheckedAt(request.checkedAt());
        CheckIn checkIn = new CheckIn(
                currentMembership.getCareCircle(),
                request.status(),
                checkedAt,
                currentUser
        );
        checkIn.setNote(normalizeOptional(request.note()));

        return checkInMapper.toResponse(checkInRepository.save(checkIn));
    }

    /**
     * Lists check-ins visible to an active care circle member.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return ordered check-in responses for the circle.
     */
    @Transactional(readOnly = true)
    public List<CheckInResponse> listCheckIns(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        return checkInRepository.findByCareCircle_Id(careCircleId)
                .stream()
                .sorted(CHECK_IN_LIST_ORDER)
                .map(checkInMapper::toResponse)
                .toList();
    }

    private OffsetDateTime resolveCheckedAt(OffsetDateTime requestedCheckedAt) {
        OffsetDateTime checkedAt = requestedCheckedAt == null ? OffsetDateTime.now() : requestedCheckedAt;
        if (checkedAt.isAfter(OffsetDateTime.now())) {
            throw new InvalidRequestException("checkedAt must not be in the future.");
        }
        return checkedAt;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
