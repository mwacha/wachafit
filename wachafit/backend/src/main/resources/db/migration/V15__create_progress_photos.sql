CREATE TABLE progress_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID NOT NULL REFERENCES users(id),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    storage_key VARCHAR(255) NOT NULL,
    taken_at    DATE NOT NULL,
    notes       VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
