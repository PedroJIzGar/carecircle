package com.carecircle.api.privacy.service;

import com.carecircle.api.privacy.entity.AuditLog;
import com.carecircle.api.privacy.entity.ConsentRecord;
import com.carecircle.api.privacy.repository.AuditLogRepository;
import com.carecircle.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Writes minimal audit entries for privacy-sensitive events.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final String CONSENT_RECORD_ENTITY = "CONSENT_RECORD";

    private final AuditLogRepository auditLogRepository;

    /**
     * Records a consent acceptance event.
     *
     * @param actorUser user who accepted the document.
     * @param consentRecord created or active consent record.
     */
    public void recordConsentAccepted(User actorUser, ConsentRecord consentRecord) {
        auditLogRepository.save(new AuditLog(
                actorUser,
                "CONSENT_ACCEPTED",
                CONSENT_RECORD_ENTITY,
                consentRecord.getId(),
                Map.of(
                        "documentType", consentRecord.getConsentType().name(),
                        "version", consentRecord.getLegalDocument().getVersion()
                )
        ));
    }

    /**
     * Records a consent revocation event.
     *
     * @param actorUser user who revoked the consent.
     * @param consentRecord revoked consent record.
     */
    public void recordConsentRevoked(User actorUser, ConsentRecord consentRecord) {
        auditLogRepository.save(new AuditLog(
                actorUser,
                "CONSENT_REVOKED",
                CONSENT_RECORD_ENTITY,
                consentRecord.getId(),
                Map.of(
                        "documentType", consentRecord.getConsentType().name(),
                        "version", consentRecord.getLegalDocument().getVersion()
                )
        ));
    }
}
