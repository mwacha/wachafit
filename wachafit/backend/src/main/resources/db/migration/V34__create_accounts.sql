CREATE TABLE accounts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Um Account por e-mail DISTINTO já existente em users. Dedup necessário: accounts.email é
-- único globalmente, mas o mesmo e-mail pode hoje aparecer em mais de um User (tenants
-- diferentes). Nesse caso (raro), os dois Users passam a compartilhar a mesma conta/senha
-- após a Task 2 — ver Global Constraints do plano e a seção 1 do spec.
INSERT INTO accounts (name, email, password_hash)
SELECT DISTINCT ON (email) name, email, password_hash
FROM users
ORDER BY email, created_at ASC;
