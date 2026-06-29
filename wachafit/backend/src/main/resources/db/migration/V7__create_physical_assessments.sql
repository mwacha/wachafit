CREATE TABLE physical_assessments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL REFERENCES users(id),
    assessed_by  UUID NOT NULL REFERENCES users(id),
    assessed_at  DATE NOT NULL,
    weight_kg    NUMERIC(5,2),
    height_cm    NUMERIC(5,2),
    body_fat_pct NUMERIC(4,1),
    bmi          NUMERIC(4,1),
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_assessments_student_date ON physical_assessments(student_id, assessed_at);
