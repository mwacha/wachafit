CREATE TABLE student_health (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL UNIQUE REFERENCES users(id),
    has_heart_condition   BOOLEAN NOT NULL DEFAULT false,
    has_diabetes          BOOLEAN NOT NULL DEFAULT false,
    has_hypertension      BOOLEAN NOT NULL DEFAULT false,
    medications           TEXT,
    physical_restrictions TEXT,
    parq_signed_at        DATE,
    notes                 TEXT,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
