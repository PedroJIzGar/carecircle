package com.carecircle.api.companionrequests.service;

import com.carecircle.api.auth.dto.SupabaseUserClaims;
import com.carecircle.api.companionrequests.dto.CompanionRequestResponse;
import com.carecircle.api.companionrequests.dto.CreateCompanionRequestRequest;
import com.carecircle.api.companionrequests.entity.CompanionRequest;
import com.carecircle.api.companionrequests.entity.CompanionRequestStatus;
import com.carecircle.api.companionrequests.mapper.CompanionRequestMapper;
import com.carecircle.api.companionrequests.repository.CompanionRequestRepository;
import com.carecircle.api.members.entity.CircleMember;
import com.carecircle.api.members.entity.CircleRole;
import com.carecircle.api.members.service.CircleMembershipAccessService;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import com.carecircle.api.privacy.service.ConsentRequirementService;
import com.carecircle.api.shared.exception.ForbiddenOperationException;
import com.carecircle.api.shared.exception.ResourceConflictException;
import com.carecircle.api.shared.exception.ResourceNotFoundException;
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
 * Application service for family companion request workflows.
 */
@Service
@RequiredArgsConstructor
public class CompanionRequestService {

    private static final String WRITE_FORBIDDEN_MESSAGE =
            "Only main caregivers and collaborators can manage companion requests.";
    private static final String COMPANION_CONSENT_REQUIRED_MESSAGE =
            "Companion request requires accepted companion consent and data sharing consent.";
    private static final List<LegalDocumentType> REQUIRED_COMPANION_CONSENTS = List.of(
            LegalDocumentType.COMPANION_CONSENT,
            LegalDocumentType.COMPANION_DATA_SHARING
    );
    private static final Comparator<CompanionRequest> REQUEST_LIST_ORDER = Comparator
            .comparingInt((CompanionRequest request) -> getStatusOrder(request.getStatus()))
            .thenComparing(CompanionRequest::getRequestedForDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(CompanionRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final UserService userService;
    private final CircleMembershipAccessService circleMembershipAccessService;
    private final ConsentRequirementService consentRequirementService;
    private final CompanionRequestRepository companionRequestRepository;
    private final CompanionRequestMapper companionRequestMapper;

    /**
     * Creates a family companion request inside a care circle.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param request validated companion request creation body.
     * @return created companion request response.
     */
    @Transactional
    public CompanionRequestResponse createCompanionRequest(
            SupabaseUserClaims claims,
            UUID careCircleId,
            CreateCompanionRequestRequest request
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        CircleMember currentMembership = getWritableMembership(careCircleId, currentUser);
        consentRequirementService.requireActiveConsents(
                currentUser,
                REQUIRED_COMPANION_CONSENTS,
                COMPANION_CONSENT_REQUIRED_MESSAGE
        );

        CompanionRequest companionRequest = new CompanionRequest(
                currentMembership.getCareCircle(),
                currentUser,
                request.requestedForDate(),
                normalizeRequired(request.timeWindow()),
                normalizeRequired(request.location())
        );
        companionRequest.setReason(normalizeOptional(request.reason()));
        companionRequest.setNotes(normalizeOptional(request.notes()));

        return companionRequestMapper.toResponse(companionRequestRepository.save(companionRequest));
    }

    /**
     * Lists companion requests visible to an active care circle member.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @return ordered companion requests for the circle.
     */
    @Transactional(readOnly = true)
    public List<CompanionRequestResponse> listCompanionRequests(SupabaseUserClaims claims, UUID careCircleId) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);

        return companionRequestRepository.findByCareCircle_Id(careCircleId)
                .stream()
                .sorted(REQUEST_LIST_ORDER)
                .map(companionRequestMapper::toResponse)
                .toList();
    }

    /**
     * Cancels an active companion request without deleting it.
     *
     * @param claims normalized claims extracted from a validated Supabase JWT.
     * @param careCircleId requested care circle identifier.
     * @param requestId requested companion request identifier.
     * @return cancelled companion request response.
     */
    @Transactional
    public CompanionRequestResponse cancelCompanionRequest(
            SupabaseUserClaims claims,
            UUID careCircleId,
            UUID requestId
    ) {
        User currentUser = userService.findOrCreateUserFromSupabaseClaims(claims);
        getWritableMembership(careCircleId, currentUser);

        CompanionRequest companionRequest = companionRequestRepository.findByIdAndCareCircle_Id(requestId, careCircleId)
                .orElseThrow(() -> new ResourceNotFoundException("Companion request not found."));

        if (companionRequest.getStatus() != CompanionRequestStatus.REQUESTED) {
            throw new ResourceConflictException("Only requested companion requests can be cancelled.");
        }

        companionRequest.setStatus(CompanionRequestStatus.CANCELLED);
        companionRequest.setCancelledAt(OffsetDateTime.now());
        companionRequest.setCancelledByUser(currentUser);

        return companionRequestMapper.toResponse(companionRequestRepository.save(companionRequest));
    }

    private CircleMember getWritableMembership(UUID careCircleId, User currentUser) {
        CircleMember membership = circleMembershipAccessService.getActiveMembershipOrThrow(careCircleId, currentUser);
        if (membership.getRole() == CircleRole.OBSERVER) {
            throw new ForbiddenOperationException(WRITE_FORBIDDEN_MESSAGE);
        }
        return membership;
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static int getStatusOrder(CompanionRequestStatus status) {
        return switch (status) {
            case REQUESTED -> 0;
            case CANCELLED -> 1;
        };
    }
}
