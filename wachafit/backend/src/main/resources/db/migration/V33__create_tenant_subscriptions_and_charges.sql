-- 1. Dados de empresa no tenant (nullable — tenants criados pelo SUPER_ADMIN via
--    POST /api/super/tenants, Task 8, podem não preenchê-los de imediato)
ALTER TABLE tenants ADD COLUMN cnpj VARCHAR(14) UNIQUE;
ALTER TABLE tenants ADD COLUMN phone VARCHAR(20);

-- 2. Assinatura do tenant a um SaasPlan
CREATE TABLE tenant_subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    saas_plan_id        UUID NOT NULL REFERENCES saas_plans(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'TRIALING',
    trial_ends_at       TIMESTAMPTZ,
    current_period_end  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Cobranças da assinatura do tenant
CREATE TABLE tenant_charges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    subscription_id UUID NOT NULL REFERENCES tenant_subscriptions(id),
    amount          NUMERIC(10,2) NOT NULL,
    due_date        DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method  VARCHAR(20),
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Backfill: Personal Studio já é cliente ativo hoje — assinatura ACTIVE,
--    sem trial e sem cobrança retroativa (a próxima cobrança nasce do ciclo normal).
INSERT INTO tenant_subscriptions (tenant_id, saas_plan_id, status, trial_ends_at, current_period_end)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000102',
    'ACTIVE',
    NULL,
    now() + interval '1 month'
);
