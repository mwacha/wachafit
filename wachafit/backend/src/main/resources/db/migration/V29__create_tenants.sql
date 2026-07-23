CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(120) NOT NULL,
    slug       VARCHAR(60)  NOT NULL UNIQUE
                   CHECK (slug ~ '^[a-z0-9][a-z0-9\-]*[a-z0-9]$'),
    active     BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Tenant padrão para dados existentes: a academia Personal Studio,
-- que já usa o sistema hoje, herda todos os dados atuais do banco.
INSERT INTO tenants (id, name, slug)
VALUES ('00000000-0000-0000-0000-000000000001', 'Personal Studio', 'personal-studio');
