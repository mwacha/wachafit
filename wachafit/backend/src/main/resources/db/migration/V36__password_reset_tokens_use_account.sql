ALTER TABLE password_reset_tokens ADD COLUMN account_id UUID REFERENCES accounts(id);

UPDATE password_reset_tokens t SET account_id = u.account_id
FROM users u
WHERE t.user_id = u.id;

ALTER TABLE password_reset_tokens ALTER COLUMN account_id SET NOT NULL;
ALTER TABLE password_reset_tokens DROP COLUMN user_id;
