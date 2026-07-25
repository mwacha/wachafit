-- backend/src/main/resources/db/migration/V35__add_account_id_to_users.sql

-- 1. Adicionar coluna account_id (nullable inicialmente para backfill)
ALTER TABLE users ADD COLUMN account_id UUID REFERENCES accounts(id);

-- 2. Backfill: liga cada User ao Account do mesmo e-mail (criado na V34)
UPDATE users u SET account_id = a.id
FROM accounts a
WHERE a.email = u.email;

-- 3. Tornar NOT NULL após o backfill
ALTER TABLE users ALTER COLUMN account_id SET NOT NULL;

-- 4. Remover name, email e password_hash de users (migraram para accounts).
--    Postgres remove automaticamente a constraint users_email_tenant_unique (V30) e
--    qualquer índice que dependa exclusivamente da coluna email ao dropá-la — não é
--    necessário (nem seguro, sem saber o nome exato gerado) fazer DROP CONSTRAINT antes.
--    DROP COLUMN name é necessário aqui (não estava no rascunho original do brief):
--    User.java não mapeia mais nenhum campo "name" (getName() passou a delegar para
--    account.getName()), então o Hibernate nunca inclui essa coluna no INSERT — a
--    coluna "name" antiga de users, ainda NOT NULL, quebraria toda inserção de User.
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users DROP COLUMN email;
ALTER TABLE users DROP COLUMN password_hash;
