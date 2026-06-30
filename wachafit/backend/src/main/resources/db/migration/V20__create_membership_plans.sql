CREATE TABLE membership_plans (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(100) NOT NULL,
    description          TEXT,
    duration_months      INT NOT NULL,
    price                NUMERIC(10,2) NOT NULL,
    max_classes_per_week INT,
    active               BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
