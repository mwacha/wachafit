CREATE TABLE schedules (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_class_id UUID REFERENCES group_classes(id),
    trainer_id     UUID NOT NULL REFERENCES users(id),
    type           VARCHAR(20) NOT NULL CHECK (type IN ('CLASS', 'PERSONAL')),
    starts_at      TIMESTAMPTZ NOT NULL,
    ends_at        TIMESTAMPTZ NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                       CHECK (status IN ('OPEN', 'FULL', 'CANCELLED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_schedules_starts_at        ON schedules(starts_at);
CREATE INDEX idx_schedules_trainer_starts   ON schedules(trainer_id, starts_at);
