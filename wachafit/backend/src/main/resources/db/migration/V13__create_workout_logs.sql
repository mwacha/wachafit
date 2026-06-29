CREATE TABLE workout_logs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id            UUID NOT NULL REFERENCES users(id),
    exercise_id           UUID NOT NULL REFERENCES exercises(id),
    workout_plan_item_id  UUID REFERENCES workout_plan_items(id),
    performed_at          DATE NOT NULL,
    sets                  INT,
    reps                  INT,
    load_kg               NUMERIC(6,2),
    notes                 VARCHAR(200),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workout_logs_student_exercise_date ON workout_logs(student_id, exercise_id, performed_at);
