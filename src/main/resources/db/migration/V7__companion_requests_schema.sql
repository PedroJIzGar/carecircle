CREATE TABLE partner_organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_partner_organizations_name UNIQUE (name),
    CONSTRAINT ck_partner_organizations_status CHECK (status IN ('VERIFIED', 'INACTIVE'))
);

CREATE INDEX idx_partner_organizations_status ON partner_organizations (status);

COMMENT ON TABLE partner_organizations IS 'Verified partner organizations that can receive companion requests in future workflows.';
COMMENT ON COLUMN partner_organizations.status IS 'Only verified partner organizations should receive companion request referrals.';

CREATE TABLE companion_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    requested_for_date DATE NOT NULL,
    time_window VARCHAR(160) NOT NULL,
    location VARCHAR(255) NOT NULL,
    reason VARCHAR(500),
    notes VARCHAR(1000),
    partner_organization_id UUID,
    submitted_to_partner_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_companion_requests_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_companion_requests_requested_by_user FOREIGN KEY (requested_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_companion_requests_partner_organization FOREIGN KEY (partner_organization_id) REFERENCES partner_organizations (id),
    CONSTRAINT fk_companion_requests_cancelled_by_user FOREIGN KEY (cancelled_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_companion_requests_status CHECK (status IN ('REQUESTED', 'CANCELLED')),
    CONSTRAINT ck_companion_requests_cancelled_state CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by_user_id IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancelled_at IS NULL AND cancelled_by_user_id IS NULL)
    )
);

CREATE INDEX idx_companion_requests_care_circle_id ON companion_requests (care_circle_id);
CREATE INDEX idx_companion_requests_care_circle_status ON companion_requests (care_circle_id, status);
CREATE INDEX idx_companion_requests_requested_for_date ON companion_requests (care_circle_id, requested_for_date);
CREATE INDEX idx_companion_requests_requested_by_user_id ON companion_requests (requested_by_user_id);
CREATE INDEX idx_companion_requests_partner_organization_id ON companion_requests (partner_organization_id);

COMMENT ON TABLE companion_requests IS 'Family companion requests that may be referred to verified partner organizations.';
COMMENT ON COLUMN companion_requests.location IS 'Family-entered location or meeting context for the companion request.';
COMMENT ON COLUMN companion_requests.partner_organization_id IS 'Optional verified partner organization for future referral workflows. CareCircle does not assign volunteers directly.';
COMMENT ON COLUMN companion_requests.submitted_to_partner_at IS 'Future workflow timestamp for partner referral. No direct volunteer assignment is represented here.';
