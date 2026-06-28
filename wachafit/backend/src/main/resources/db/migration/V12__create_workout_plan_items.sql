CREATE TABLE workout_plan_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_plan_id   UUID NOT NULL REFERENCES workout_plans(id),
    exercise_id       UUID NOT NULL REFERENCES exercises(id),
    division          VARCHAR(10),
    sets              INT NOT NULL,
    reps              VARCHAR(20) NOT NULL,
    suggested_load_kg NUMERIC(6,2),
    rest_seconds      INT,
    order_index       INT NOT NULL,
    notes             VARCHAR(200)
);
