CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    care_circle_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    due_at TIMESTAMP WITH TIME ZONE,
    assigned_to_user_id UUID,
    created_by_user_id UUID NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_tasks_care_circle FOREIGN KEY (care_circle_id) REFERENCES care_circles (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assigned_to_user FOREIGN KEY (assigned_to_user_id) REFERENCES users (id),
    CONSTRAINT fk_tasks_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_tasks_completed_by_user FOREIGN KEY (completed_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_tasks_status CHECK (status IN ('OPEN', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH')),
    CONSTRAINT ck_tasks_completed_state CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status <> 'COMPLETED' AND completed_at IS NULL AND completed_by_user_id IS NULL)
    )
);

CREATE INDEX idx_tasks_care_circle_id ON tasks (care_circle_id);
CREATE INDEX idx_tasks_care_circle_status ON tasks (care_circle_id, status);
CREATE INDEX idx_tasks_care_circle_due_at ON tasks (care_circle_id, due_at);
CREATE INDEX idx_tasks_assigned_to_user_id ON tasks (assigned_to_user_id);
CREATE INDEX idx_tasks_created_by_user_id ON tasks (created_by_user_id);

COMMENT ON TABLE tasks IS 'Non-clinical family coordination tasks inside a care circle.';
COMMENT ON COLUMN tasks.description IS 'Family coordination notes only. Do not store diagnoses, treatment recommendations, or medical decisions here.';
COMMENT ON COLUMN tasks.assigned_to_user_id IS 'Optional active circle member responsible for the task.';
