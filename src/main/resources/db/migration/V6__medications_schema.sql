CREATE TABLE medication_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    medication_name VARCHAR(160) NOT NULL,
    dosage_text VARCHAR(160),
    schedule_text VARCHAR(255) NOT NULL,
    instructions VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE,
    end_date DATE,
    created_by_user_id UUID NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    archived_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_medication_reminders_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_medication_reminders_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_medication_reminders_archived_by_user FOREIGN KEY (archived_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_medication_reminders_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_medication_reminders_date_range CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_medication_reminders_archived_state CHECK (
        (status = 'ARCHIVED' AND archived_at IS NOT NULL AND archived_by_user_id IS NOT NULL)
        OR (status <> 'ARCHIVED' AND archived_at IS NULL AND archived_by_user_id IS NULL)
    )
);

CREATE INDEX idx_medication_reminders_care_circle_id ON medication_reminders (care_circle_id);
CREATE INDEX idx_medication_reminders_care_circle_status ON medication_reminders (care_circle_id, status);
CREATE INDEX idx_medication_reminders_created_by_user_id ON medication_reminders (created_by_user_id);

COMMENT ON TABLE medication_reminders IS 'Family-entered medication reminders inside a care circle.';
COMMENT ON COLUMN medication_reminders.medication_name IS 'Medication name entered by the family. CareCircle does not prescribe or recommend medication.';
COMMENT ON COLUMN medication_reminders.dosage_text IS 'Free-text dosage label entered by the family. CareCircle does not validate or recommend dosage.';
COMMENT ON COLUMN medication_reminders.schedule_text IS 'Free-text schedule label entered by the family. CareCircle does not calculate clinical schedules.';
COMMENT ON COLUMN medication_reminders.instructions IS 'Family coordination notes only. Do not store diagnoses, treatment recommendations, or medical decisions here.';

CREATE TABLE medication_intake_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    reminder_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    note VARCHAR(1000),
    recorded_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_medication_intake_logs_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_medication_intake_logs_reminder FOREIGN KEY (reminder_id) REFERENCES medication_reminders (id),
    CONSTRAINT fk_medication_intake_logs_recorded_by_user FOREIGN KEY (recorded_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_medication_intake_logs_status CHECK (status IN ('TAKEN', 'SKIPPED'))
);

CREATE INDEX idx_medication_intake_logs_care_circle_id ON medication_intake_logs (care_circle_id);
CREATE INDEX idx_medication_intake_logs_reminder_id ON medication_intake_logs (reminder_id);
CREATE INDEX idx_medication_intake_logs_care_circle_occurred_at ON medication_intake_logs (care_circle_id, occurred_at);
CREATE INDEX idx_medication_intake_logs_recorded_by_user_id ON medication_intake_logs (recorded_by_user_id);

COMMENT ON TABLE medication_intake_logs IS 'Manual family logs for medication reminders.';
COMMENT ON COLUMN medication_intake_logs.status IS 'Family-recorded intake status. CareCircle does not decide whether medication should be taken or skipped.';
COMMENT ON COLUMN medication_intake_logs.note IS 'Family coordination note only. Do not store diagnoses, treatment recommendations, or medical decisions here.';
