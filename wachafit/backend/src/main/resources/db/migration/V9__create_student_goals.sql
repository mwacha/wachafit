CREATE TABLE student_goals (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL REFERENCES users(id),
    created_by   UUID NOT NULL REFERENCES users(id),
    description  VARCHAR(200) NOT NULL,
    metric       VARCHAR(40),
    target_value NUMERIC(8,2),
    target_date  DATE,
    status       VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                     CHECK (status IN ('IN_PROGRESS','ACHIEVED','EXPIRED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
