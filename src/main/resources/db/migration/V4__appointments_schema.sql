CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    location VARCHAR(255),
    notes VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID NOT NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_appointments_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_cancelled_by_user FOREIGN KEY (cancelled_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_appointments_status CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT ck_appointments_time_range CHECK (ends_at IS NULL OR ends_at > starts_at),
    CONSTRAINT ck_appointments_cancelled_state CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by_user_id IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancelled_at IS NULL AND cancelled_by_user_id IS NULL)
    )
);

CREATE INDEX idx_appointments_care_circle_id ON appointments (care_circle_id);
CREATE INDEX idx_appointments_care_circle_status ON appointments (care_circle_id, status);
CREATE INDEX idx_appointments_care_circle_starts_at ON appointments (care_circle_id, starts_at);
CREATE INDEX idx_appointments_created_by_user_id ON appointments (created_by_user_id);

COMMENT ON TABLE appointments IS 'Non-clinical appointments coordinated by family members inside a care circle.';
COMMENT ON COLUMN appointments.notes IS 'Family coordination notes only. Do not store diagnoses, treatment recommendations, or medical decisions here.';
COMMENT ON COLUMN appointments.location IS 'Optional appointment location or channel, such as an address, clinic name, or video call link.';
