CREATE TABLE saas_plans (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(100) NOT NULL,
    description            TEXT,
    price                  NUMERIC(10,2) NOT NULL,
    billing_period_months  INT NOT NULL DEFAULT 1,
    max_users              INT,
    active                 BOOLEAN NOT NULL DEFAULT true,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Planos padrão disponíveis no cadastro self-service (Task 12).
-- IDs fixos para permitir referência determinística no seed da Task 11.
INSERT INTO saas_plans (id, name, description, price, billing_period_months, max_users, active) VALUES
    ('00000000-0000-0000-0000-000000000101', 'Starter', 'Ideal para academias pequenas', 149.90, 1, 5, true),
    ('00000000-0000-0000-0000-000000000102', 'Pro', 'Para academias em crescimento, sem limite de turmas', 299.90, 1, 20, true);
