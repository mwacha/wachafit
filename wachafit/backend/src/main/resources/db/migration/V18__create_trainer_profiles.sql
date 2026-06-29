CREATE TABLE trainer_profiles (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL UNIQUE REFERENCES users(id),
    cref              VARCHAR(20),
    specialties       TEXT,
    bio               TEXT,
    profile_photo_key VARCHAR(255),
    contract_type     VARCHAR(20) CHECK (contract_type IN ('CLT','PJ','FREELANCE')),
    admission_date    DATE,
    commission_type   VARCHAR(20) CHECK (commission_type IN ('FIXED','PERCENTAGE')),
    commission_value  NUMERIC(8,2),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
