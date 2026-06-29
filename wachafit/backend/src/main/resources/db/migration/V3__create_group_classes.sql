CREATE TABLE group_classes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(120) NOT NULL,
    description      TEXT,
    capacity         INT NOT NULL,
    duration_minutes INT NOT NULL,
    trainer_id       UUID NOT NULL REFERENCES users(id),
    active           BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
