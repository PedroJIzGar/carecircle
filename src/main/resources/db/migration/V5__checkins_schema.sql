CREATE TABLE checkins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    note VARCHAR(1000),
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_checkins_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_checkins_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_checkins_status CHECK (status IN ('OK', 'NEEDS_ATTENTION', 'NO_RESPONSE'))
);

CREATE INDEX idx_checkins_care_circle_id ON checkins (care_circle_id);
CREATE INDEX idx_checkins_care_circle_checked_at ON checkins (care_circle_id, checked_at);
CREATE INDEX idx_checkins_created_by_user_id ON checkins (created_by_user_id);

COMMENT ON TABLE checkins IS 'Non-clinical family check-ins inside a care circle.';
COMMENT ON COLUMN checkins.status IS 'Family coordination signal only. It is not a diagnosis or clinical assessment.';
COMMENT ON COLUMN checkins.note IS 'Family coordination note only. Do not store diagnoses, treatment recommendations, or medical decisions here.';
