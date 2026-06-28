CREATE TABLE personal_records (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES users(id),
    exercise_id    UUID NOT NULL REFERENCES exercises(id),
    record_load_kg NUMERIC(6,2) NOT NULL,
    achieved_at    DATE NOT NULL,
    UNIQUE (student_id, exercise_id)
);
