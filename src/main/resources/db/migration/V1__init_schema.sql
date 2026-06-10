CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supabase_user_id VARCHAR(128) NOT NULL,
    full_name VARCHAR(160),
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(32),
    avatar_url VARCHAR(1024),
    global_role VARCHAR(32) NOT NULL DEFAULT 'USER',
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    terms_accepted_at TIMESTAMP WITH TIME ZONE,
    privacy_accepted_at TIMESTAMP WITH TIME ZONE,
    medical_disclaimer_accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_login_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uq_users_supabase_user_id UNIQUE (supabase_user_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_global_role CHECK (global_role IN ('USER', 'ADMIN', 'PARTNER_USER')),
    CONSTRAINT ck_users_account_status CHECK (account_status IN ('ACTIVE', 'DISABLED', 'PENDING_VERIFICATION'))
);

CREATE INDEX idx_users_account_status ON users (account_status);

COMMENT ON TABLE users IS 'Internal CareCircle users synchronized from Supabase Auth.';
COMMENT ON COLUMN users.supabase_user_id IS 'Supabase Auth subject claim (JWT sub).';
COMMENT ON COLUMN users.global_role IS 'Application-level role only. Circle roles are stored in circle membership tables.';

CREATE TABLE legal_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type VARCHAR(64) NOT NULL,
    version VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content_url VARCHAR(1024),
    content_sha256 VARCHAR(64),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_legal_documents_type_version UNIQUE (document_type, version),
    CONSTRAINT ck_legal_documents_type CHECK (
        document_type IN (
            'TERMS_OF_SERVICE',
            'PRIVACY_POLICY',
            'MEDICAL_DISCLAIMER',
            'COMPANION_CONSENT',
            'COMPANION_DATA_SHARING'
        )
    )
);

CREATE INDEX idx_legal_documents_type_active ON legal_documents (document_type, is_active);

COMMENT ON TABLE legal_documents IS 'Versioned legal and consent documents accepted by users.';

CREATE TABLE consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    legal_document_id UUID NOT NULL,
    consent_type VARCHAR(64) NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    revoked_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_consent_records_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_consent_records_legal_document FOREIGN KEY (legal_document_id) REFERENCES legal_documents (id),
    CONSTRAINT ck_consent_records_type CHECK (
        consent_type IN (
            'TERMS_OF_SERVICE',
            'PRIVACY_POLICY',
            'MEDICAL_DISCLAIMER',
            'COMPANION_CONSENT',
            'COMPANION_DATA_SHARING'
        )
    )
);

CREATE INDEX idx_consent_records_user_id ON consent_records (user_id);
CREATE INDEX idx_consent_records_legal_document_id ON consent_records (legal_document_id);
CREATE UNIQUE INDEX uq_consent_records_active_user_document
    ON consent_records (user_id, legal_document_id)
    WHERE revoked_at IS NULL;

COMMENT ON TABLE consent_records IS 'User acceptances for versioned legal documents and explicit consents.';

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_audit_logs_actor_user FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at);

COMMENT ON TABLE audit_logs IS 'Basic audit trail for privacy, consent, and sensitive MVP actions.';
COMMENT ON COLUMN audit_logs.metadata IS 'Minimal JSON metadata. Do not store private data snapshots here.';
