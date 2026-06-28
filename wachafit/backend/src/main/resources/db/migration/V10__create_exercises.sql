CREATE TABLE exercises (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(120) NOT NULL,
    muscle_group VARCHAR(60) NOT NULL,
    description  TEXT,
    video_url    VARCHAR(255),
    active       BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_exercises_muscle_group ON exercises(muscle_group);
