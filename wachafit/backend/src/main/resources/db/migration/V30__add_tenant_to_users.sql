-- 1. Adicionar coluna tenant_id (nullable inicialmente para backfill)
ALTER TABLE users ADD COLUMN tenant_id UUID REFERENCES tenants(id);

-- 2. Backfill: todos os usuários existentes vão para o tenant default
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001';

-- 3. Tornar NOT NULL após o backfill
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

-- 4. Remover a constraint UNIQUE antiga em email
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;

-- 5. Adicionar nova constraint UNIQUE (email, tenant_id)
ALTER TABLE users ADD CONSTRAINT users_email_tenant_unique UNIQUE (email, tenant_id);

-- 6. Índice para busca de login
CREATE INDEX idx_users_email_tenant ON users(email, tenant_id);
