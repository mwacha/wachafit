CREATE TABLE payment_charges (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id      UUID NOT NULL REFERENCES member_subscriptions(id),
    student_id           UUID NOT NULL REFERENCES users(id),
    amount               NUMERIC(10,2) NOT NULL,
    due_date             DATE NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING','PAID','OVERDUE','CANCELLED')),
    paid_at              TIMESTAMPTZ,
    payment_method       VARCHAR(20) CHECK (payment_method IN ('BOLETO','PIX','CARD','CASH')),
    gateway              VARCHAR(20) CHECK (gateway IN ('PAGSEGURO','MERCADOPAGO','MANUAL')),
    external_charge_id   VARCHAR(255),
    external_payment_url TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_charges_student_status ON payment_charges(student_id, status);
CREATE INDEX idx_charges_due_date       ON payment_charges(due_date, status);
