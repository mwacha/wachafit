CREATE TABLE member_subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID NOT NULL REFERENCES users(id),
    plan_id             UUID NOT NULL REFERENCES membership_plans(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','SUSPENDED','CANCELLED','EXPIRED')),
    started_at          DATE NOT NULL,
    expires_at          DATE NOT NULL,
    cancelled_at        DATE,
    cancellation_reason TEXT,
    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_student_status ON member_subscriptions(student_id, status);
