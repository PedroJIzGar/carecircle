package com.carecircle.api.privacy;

import com.carecircle.api.privacy.entity.ConsentRecord;
import com.carecircle.api.privacy.entity.LegalDocument;
import com.carecircle.api.privacy.entity.LegalDocumentType;
import com.carecircle.api.privacy.repository.ConsentRecordRepository;
import com.carecircle.api.privacy.repository.LegalDocumentRepository;
import com.carecircle.api.shared.audit.repository.AuditLogRepository;
import com.carecircle.api.users.entity.User;
import com.carecircle.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrivacyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    @Autowired
    private ConsentRecordRepository consentRecordRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void listLegalDocumentsReturnsSeededActiveDocuments() throws Exception {
        User user = createUser("privacy-docs", "Privacy Docs User");

        mockMvc.perform(get("/privacy/legal-documents")
                        .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].documentType").value("TERMS_OF_SERVICE"))
                .andExpect(jsonPath("$[0].version").value("MVP-0.1"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].documentType").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$[2].documentType").value("MEDICAL_DISCLAIMER"))
                .andExpect(jsonPath("$[3].documentType").value("COMPANION_CONSENT"))
                .andExpect(jsonPath("$[4].documentType").value("COMPANION_DATA_SHARING"));
    }

    @Test
    void getMyPrivacyStatusShowsCurrentDocumentAcceptanceState() throws Exception {
        User user = createUser("privacy-status", "Privacy Status User");
        LegalDocument privacyPolicy = getActiveDocument(LegalDocumentType.PRIVACY_POLICY);
        consentRecordRepository.save(new ConsentRecord(user, privacyPolicy));

        mockMvc.perform(get("/privacy/me")
                        .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(5)))
                .andExpect(jsonPath("$.documents[0].documentType").value("TERMS_OF_SERVICE"))
                .andExpect(jsonPath("$.documents[0].required").value(true))
                .andExpect(jsonPath("$.documents[0].accepted").value(false))
                .andExpect(jsonPath("$.documents[1].documentType").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.documents[1].required").value(true))
                .andExpect(jsonPath("$.documents[1].accepted").value(true))
                .andExpect(jsonPath("$.documents[1].consentRecordId", notNullValue()))
                .andExpect(jsonPath("$.documents[3].documentType").value("COMPANION_CONSENT"))
                .andExpect(jsonPath("$.documents[3].required").value(false));
    }

    @Test
    void acceptConsentCreatesRecordUpdatesUserTimestampAndWritesAuditLog() throws Exception {
        User user = createUser("privacy-accept", "Privacy Accept User");
        LegalDocument privacyPolicy = getActiveDocument(LegalDocumentType.PRIVACY_POLICY);

        mockMvc.perform(post("/privacy/consents")
                        .with(jwtFor(user))
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                        .header("User-Agent", "CareCircleTest/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "PRIVACY_POLICY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalDocumentId").value(privacyPolicy.getId().toString()))
                .andExpect(jsonPath("$.documentType").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.consentType").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$.acceptedAt", notNullValue()));

        List<ConsentRecord> activeConsents = consentRecordRepository
                .findByUser_IdAndLegalDocument_IdInAndRevokedAtIsNull(user.getId(), List.of(privacyPolicy.getId()));
        assertThat(activeConsents)
                .singleElement()
                .satisfies(consent -> {
                    assertThat(consent.getIpAddress()).isEqualTo("203.0.113.10");
                    assertThat(consent.getUserAgent()).isEqualTo("CareCircleTest/1.0");
                });

        assertThat(userRepository.findById(user.getId()))
                .isPresent()
                .get()
                .extracting(User::getPrivacyAcceptedAt)
                .isNotNull();

        UUID consentRecordId = activeConsents.getFirst().getId();
        assertThat(auditLogRepository.findAll())
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getAction()).isEqualTo("CONSENT_ACCEPTED");
                    assertThat(auditLog.getEntityType()).isEqualTo("CONSENT_RECORD");
                    assertThat(auditLog.getEntityId()).isEqualTo(consentRecordId);
                    assertThat(auditLog.getActorUser().getId()).isEqualTo(user.getId());
                    assertThat(auditLog.getMetadata()).containsEntry("documentType", "PRIVACY_POLICY");
                });
    }

    @Test
    void acceptConsentIsIdempotentForTheCurrentActiveDocument() throws Exception {
        User user = createUser("privacy-idempotent", "Privacy Idempotent User");
        LegalDocument terms = getActiveDocument(LegalDocumentType.TERMS_OF_SERVICE);

        acceptDocument(user, LegalDocumentType.TERMS_OF_SERVICE)
                .andExpect(status().isOk());
        acceptDocument(user, LegalDocumentType.TERMS_OF_SERVICE)
                .andExpect(status().isOk());

        assertThat(consentRecordRepository
                .findByUser_IdAndLegalDocument_IdInAndRevokedAtIsNull(user.getId(), List.of(terms.getId())))
                .hasSize(1);
        assertThat(auditLogRepository.findByActionOrderByOccurredAtDesc("CONSENT_ACCEPTED"))
                .hasSize(1);
    }

    @Test
    void acceptConsentValidatesRequiredDocumentType() throws Exception {
        User user = createUser("privacy-validation", "Privacy Validation User");

        mockMvc.perform(post("/privacy/consents")
                        .with(jwtFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("documentType"));
    }

    @Test
    void revokeOptionalCompanionConsentMarksRecordRevokedAndWritesAuditLog() throws Exception {
        User user = createUser("privacy-revoke", "Privacy Revoke User");
        LegalDocument companionConsent = getActiveDocument(LegalDocumentType.COMPANION_CONSENT);
        ConsentRecord consentRecord = consentRecordRepository.save(new ConsentRecord(user, companionConsent));

        mockMvc.perform(post("/privacy/consents/{consentRecordId}/revoke", consentRecord.getId())
                        .with(jwtFor(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(consentRecord.getId().toString()))
                .andExpect(jsonPath("$.documentType").value("COMPANION_CONSENT"))
                .andExpect(jsonPath("$.revokedAt", notNullValue()));

        assertThat(consentRecordRepository.findById(consentRecord.getId()))
                .isPresent()
                .get()
                .extracting(ConsentRecord::getRevokedAt)
                .isNotNull();

        assertThat(auditLogRepository.findAll())
                .anySatisfy(auditLog -> {
                    assertThat(auditLog.getAction()).isEqualTo("CONSENT_REVOKED");
                    assertThat(auditLog.getEntityId()).isEqualTo(consentRecord.getId());
                    assertThat(auditLog.getMetadata()).containsEntry("documentType", "COMPANION_CONSENT");
                });
    }

    @Test
    void revokeRequiredLegalDocumentReturnsConflict() throws Exception {
        User user = createUser("privacy-required", "Privacy Required User");
        LegalDocument privacyPolicy = getActiveDocument(LegalDocumentType.PRIVACY_POLICY);
        ConsentRecord consentRecord = consentRecordRepository.save(new ConsentRecord(user, privacyPolicy));

        mockMvc.perform(post("/privacy/consents/{consentRecordId}/revoke", consentRecord.getId())
                        .with(jwtFor(user)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Required legal documents cannot be revoked through this endpoint."));
    }

    @Test
    void revokeConsentReturnsNotFoundWhenConsentBelongsToAnotherUser() throws Exception {
        User owner = createUser("privacy-owner", "Privacy Owner User");
        User otherUser = createUser("privacy-other", "Privacy Other User");
        LegalDocument companionConsent = getActiveDocument(LegalDocumentType.COMPANION_CONSENT);
        ConsentRecord consentRecord = consentRecordRepository.save(new ConsentRecord(owner, companionConsent));

        mockMvc.perform(post("/privacy/consents/{consentRecordId}/revoke", consentRecord.getId())
                        .with(jwtFor(otherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Consent record not found."));
    }

    @Test
    void privacyEndpointsRequireBearerAuthentication() throws Exception {
        mockMvc.perform(get("/privacy/legal-documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/privacy/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(post("/privacy/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "TERMS_OF_SERVICE"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private LegalDocument getActiveDocument(LegalDocumentType documentType) {
        return legalDocumentRepository
                .findFirstByDocumentTypeAndActiveTrueOrderByPublishedAtDescCreatedAtDesc(documentType)
                .orElseThrow();
    }

    private org.springframework.test.web.servlet.ResultActions acceptDocument(
            User user,
            LegalDocumentType documentType
    ) throws Exception {
        return mockMvc.perform(post("/privacy/consents")
                .with(jwtFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "documentType": "%s"
                        }
                        """.formatted(documentType.name())));
    }

    private RequestPostProcessor jwtFor(User user) {
        return jwt().jwt(token -> token
                .subject(user.getSupabaseUserId())
                .claim("email", user.getEmail()));
    }

    private User createUser(String prefix, String fullName) {
        User user = new User(
                UUID.randomUUID().toString(),
                prefix + "-" + UUID.randomUUID() + "@example.com"
        );
        user.setFullName(fullName);
        return userRepository.save(user);
    }
}
