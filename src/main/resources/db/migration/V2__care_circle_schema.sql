CREATE TABLE care_circles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_care_circles_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_care_circles_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_care_circles_created_by_user_id ON care_circles (created_by_user_id);
CREATE INDEX idx_care_circles_status ON care_circles (status);

COMMENT ON TABLE care_circles IS 'Family care spaces used to coordinate care around one elder profile.';
COMMENT ON COLUMN care_circles.created_by_user_id IS 'Internal user that created the care circle.';

CREATE TABLE elder_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    preferred_name VARCHAR(100),
    birth_date DATE,
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_elder_profiles_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT uq_elder_profiles_care_circle_id UNIQUE (care_circle_id)
);

CREATE INDEX idx_elder_profiles_full_name ON elder_profiles (full_name);

COMMENT ON TABLE elder_profiles IS 'Basic non-clinical elder profile associated with a care circle.';
COMMENT ON COLUMN elder_profiles.notes IS 'General family notes only. Do not store diagnoses, treatment decisions, or clinical recommendations here.';

CREATE TABLE circle_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    invited_at TIMESTAMP WITH TIME ZONE,
    joined_at TIMESTAMP WITH TIME ZONE,
    removed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_circle_members_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_circle_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_circle_members_circle_user UNIQUE (care_circle_id, user_id),
    CONSTRAINT ck_circle_members_role CHECK (role IN ('MAIN_CAREGIVER', 'COLLABORATOR', 'OBSERVER')),
    CONSTRAINT ck_circle_members_status CHECK (status IN ('ACTIVE', 'INVITED', 'REMOVED'))
);

CREATE INDEX idx_circle_members_care_circle_id ON circle_members (care_circle_id);
CREATE INDEX idx_circle_members_user_id ON circle_members (user_id);
CREATE INDEX idx_circle_members_circle_role ON circle_members (care_circle_id, role);
CREATE INDEX idx_circle_members_circle_status ON circle_members (care_circle_id, status);

COMMENT ON TABLE circle_members IS 'Membership relationship between users and care circles, including family-specific roles.';
COMMENT ON COLUMN circle_members.role IS 'Role inside this care circle only. This is separate from users.global_role.';
